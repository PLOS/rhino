#!/usr/bin/python2

# Copyright (c) 2017 Public Library of Science
#
# Permission is hereby granted, free of charge, to any person obtaining a
# copy of this software and associated documentation files (the "Software"),
# to deal in the Software without restriction, including without limitation
# the rights to use, copy, modify, merge, publish, distribute, sublicense,
# and/or sell copies of the Software, and to permit persons to whom the
# Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
# THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
# DEALINGS IN THE SOFTWARE.


"""Exercises Rhino's REST API.

This script is a quick-and-dirty integration test that exercises the REST
API with actual HTTP requests. It requires that a Tomcat instance be
running Rhino at the time the script is executed.

This script does not currently assert its expected behavior; it is not
really an automated test suite as much as a demo for a human to examine and
debug. It is supplanted by the Java unit tests, which do assert correct
behavior for the services (but not the REST API).
"""

from __future__ import print_function
from __future__ import with_statement

import json

try:
    import requests
except ImportError, e:
    print('''
        Requires third-party library: requests
        <http://docs.python-requests.org/>
        To install on a PLOS development box:
            sudo apt-get install python-pip
            sudo pip install requests
        ''')
    raise e

from articlecase import ArticleCase


"""A file path, relative to this script, to the test data location."""


SERVER_HOST = 'http://localhost:8080'

_BANNER_WIDTH = 79


def pretty_dict_repr(d):
    """Represent a dictionary with human-friendly text.

    Assuming d is of type dict, the output should be syntactically
    equivalent to repr(d), but with each key-value pair on its own,
    indented line.
    """
    lines = ['    {0!r}: {1!r},'.format(k, v) for (k, v) in sorted(d.items())]
    return '\n'.join(['{'] + lines + ['}'])

def section(*parts):
    """Print a section banner."""
    print('=' * _BANNER_WIDTH)
    print(*parts)
    print()

def report(description, response):
    """Print a description of the HTTP response."""
    print('-' * _BANNER_WIDTH)
    print(description)
    print()
    print('Status {0}: {1!r}'.format(response.status_code, response.reason))
    print('Headers:', pretty_dict_repr(response.headers))
    print()

    print('Response size:', len(response.content))
    content_lines = list(response.iter_lines())
    for (line_number, line) in enumerate(content_lines):
        if line_number > 24:
            print('...')
            print(content_lines[-1])
            break
        print(line)
    print()

def interpret_article(response):
    """Load JSON into a Python object and use some values from it."""
    article = json.loads(response.content)
    print('-' * _BANNER_WIDTH)
    print('Interpret article metadata in Python')
    print()
    print('DOI:    ', article['doi'])
    print('Title:  ', article['title'])
    print('Authors:')
    for author in article['authors']:
        print('    ' + author['fullName'])
    print()

def run_test_on_article(case):
    """Run the test for one article test case."""
    section('Running article test for', case.article_doi())

    with open(case.xml_path()) as f:
        create = requests.post(SERVER_HOST + '/articles', files={'xml': f})
    report('Create article', create)

    url_args = {'host': SERVER_HOST, 'doi': case.article_doi()}
    article_id = '{host}/articles/{doi}'.format(**url_args)
    xml_asset_id = '{host}/assetfiles/{doi}.xml'.format(**url_args)

    read_meta = requests.get(article_id)
    report('Read article metadata', read_meta)
    if (read_meta.status_code < 400):
        interpret_article(read_meta)

    read_xml = requests.get(xml_asset_id)
    report('Read article XML', read_xml)

    # Upload each asset
    for asset_case in case.assets:
        fields = {'doi': asset_case.doi(), 'ext': asset_case.extension}
        with open(asset_case.path()) as f:
            upload_asset = requests.post(SERVER_HOST + '/assetfiles',
                                         data=fields, files={'file': f})
        report('Upload asset: ' + asset_case.brief_name(), upload_asset)

    # Read each asset
    for asset_case in case.assets:
        asset_id = '{host}/assets/{doi}.{suffix}'.format(
            suffix=asset_case.suffix, **url_args)
        read_asset_meta = requests.get(asset_id)
        report('Read asset metadata: ' + asset_case.brief_name(),
               read_asset_meta)

    delete = requests.delete(article_id)
    report('Delete article', delete)

CASES = [
    ArticleCase('../resources/articles/', 'pone.0038869',
                [('g001', 'tif'),
                 ('g001', 'png_s'),
                 ('g002', 'tif'),
                 ]),
    ]

for case in CASES:
    run_test_on_article(case)
