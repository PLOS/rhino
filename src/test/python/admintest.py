#!/usr/bin/python2

# Copyright (c) 2012 by Public Library of Science
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


"""Exercises the ArticleCrudController methods in Ambra Admin TNG.

This script serves as a quick-and-dirty integration test by exercising the
Spring controllers in ways that the unit tests cannot. It requires that a
Tomcat instance be running Ambra Admin TNG at the time the script is
executed.

This script uses the same test data as the unit test suite. The list of
test cases is currently hard-coded into the script, but if it grows too
large, it could be either split into its own module or automated to use
whatever it finds in the right directories.
"""

import httplib
import os
from restclient import Request


TEST_DATA_PATH = '../resources/data/'
"""A file path, relative to this script, to the test data location."""

DOI_PREFIX = '10.1371/'

class TestArticle(object):
    """One test case of an article to manipulate."""
    def __init__(self, doi, asset_suffixes=()):
        """Create a test case for an article.

        The DOI is the actual DOI for the article; it should not have an
        '.xml' extension. Each asset suffix can be appended to the DOI to
        produce the quasi-DOI identifier of an asset that goes with th
        earticle. The asset suffixes *should* have filename extensions.
        """
        self.doi = doi
        self.asset_suffixes = asset_suffixes

    def article_doi(self):
        """Return the article's actual DOI."""
        return DOI_PREFIX + self.doi

    def article_id(self):
        """Return the article's RESTful identifier."""
        return self.article_doi() + '.xml'

    def xml_path(self):
        """Return a local file path from this script to the article's data."""
        return os.path.join(TEST_DATA_PATH, self.doi + '.xml')

    def assets(self):
        """Generate the sequence of this article's assets.

        Each yielded value is a (asset_id, asset_file) tuple. The ID is the
        full RESTful identifier for the asset, and the file is the local
        file path to the asset data.
        """
        for suffix in self.asset_suffixes:
            if not suffix.startswith('.'):
                suffix = '.' + suffix
            asset_path = self.doi + suffix
            asset_id = DOI_PREFIX + asset_path
            asset_file = os.path.join(TEST_DATA_PATH, asset_path)
            yield (asset_id, asset_file)

    def __str__(self):
        return 'TestArticle({0!r}, {1!r})'.format(self.doi, self.asset_suffixes)

TEST_ARTICLES = [
    TestArticle('journal.pone.0038869', ['g002.tif']),
    ]

_SNIPPET_SIZE = 40
"""The display size for the response body's head and tail."""

def code_message(code):
    """Translate an HTTP response code to its standard message."""
    try:
        return httplib.responses[code]
    except KeyError:
        return '(Undefined)'

def display_header_item(header_item):
    """Render one header as a user-readable string.

    The argument is either a simple string value or a (key, value) tuple.
    """
    if isinstance(header_item, str):
        return repr(header_item)
    if len(header_item) != 2:
        raise ValueError("Expected only strings and 2-tuples in headers")
    return '{0!r}: {1!r}'.format(*header_item)

def report(description, response):
    """Prepare an HTTP response for display as a string."""
    buf = [description, '',
           'HTTP Status {0}: {1}'.format(response.status,
                                         code_message(response.status))]
    headers = response.get_headers()
    if not headers:
        buf.append('No headers')
    else:
        buf.append('Headers:')
        buf += ('    ' + display_header_item(item) for item in headers)
    buf.append('')  # Skip a line before the response body

    if response.body is None:
        buf.append('No response body')
    elif len(response.body) >= 80:
        buf += ['Response size: {0}'.format(len(response.body)),
                'Response head: {0!r}'.format(response.body[ :  _SNIPPET_SIZE]),
                'Response tail: {0!r}'.format(response.body[-_SNIPPET_SIZE : ])]
    else:
        buf += ['Response body:', repr(response.body)]

    buf.append('\n')  # Two blank lines to separate reports
    return '\n'.join(buf)

def run_test_on_article(case):
    """Run the test for one article test case."""
    print 'Running article test for', case

    def article_req():
        return Request('localhost', 'article/' + case.article_id(), port=8080)
    def asset_req(asset_id):
        return Request('localhost', 'asset/' + asset_id, port=8080)

    create = article_req()
    create.set_form_file_path('file', case.xml_path())
    print report('Response to CREATE for article', create.post())

    for asset_id, asset_file in case.assets():
        create_asset = asset_req(asset_id)
        create_asset.set_form_file_path('file', asset_file)
        create_asset.set_query_parameter('assetOf', case.article_doi())
        print report('Response to CREATE for asset', create_asset.post())

    read = article_req()
    print report('Response to READ', read.get())

    for asset_id, asset_file in case.assets():
        read_asset = asset_req(asset_id)
        print report('Response to READ for asset', read_asset.get())
        delete_asset = asset_req(asset_id)
        print report('Response to DELETE for asset', delete_asset.delete())

    delete = article_req()
    print report('Response to DELETE', delete.delete())


for case in TEST_ARTICLES:
    run_test_on_article(case)
