#!/usr/bin/python2

# Copyright (c) 2012-2013 by Public Library of Science
# http://plos.org
# http://ambraproject.org
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


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

try:
    import requests
except ImportError, e:
    print('''
        Requires third-party library: requests
        <http://docs.python-requests.org/>
        To install on a PLOS development box:
            sudo apt-get install pip
            sudo pip install requests
        ''')
    raise e

from articlecase import ArticleCase


"""A file path, relative to this script, to the test data location."""


SERVER_HOST = 'http://localhost:8080/'

_BANNER_WIDTH = 79


def pretty_dict_repr(d):
    lines = ['    {0!r}: {1!r},'.format(k, v) for (k, v) in sorted(d.items())]
    return '\n'.join(['{'] + lines + ['}'])

def section(*parts):
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
        if line_number > 4:
            print('...')
            print(content_lines[-1])
            break
        print(line)
    print()

def run_test_on_article(case):
    """Run the test for one article test case."""
    section('Running article test for', case.article_doi())

    with open(case.xml_path()) as f:
        create = requests.post(SERVER_HOST + 'article', files={'file': f})
    report('Create article', create)

    article_id = 'article/' + case.article_doi()

    read = requests.get(SERVER_HOST + article_id)
    report('Read article metadata', read)

    delete = requests.delete(SERVER_HOST + article_id)
    report('Delete article', delete)

run_test_on_article(ArticleCase(
        '../resources/articles/', 'journal.pone.0038869'))
