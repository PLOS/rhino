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

from __future__ import print_function
from __future__ import with_statement

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

def report(description, response):
    """Print a description of the HTTP response."""
    print(description)
    print()
    print(response.display())

def run_test_on_article(case):
    """Run the test for one article test case."""
    print('Running article test for', case)
    print()

    def article_req():
        return Request('localhost', 'article/' + case.article_id(), port=8080)
    def asset_req(asset_id):
        return Request('localhost', 'asset/' + asset_id, port=8080)

    for i in range(2): # First create, then update
        upload = article_req()
        with open(case.xml_path()) as xml_file:
            upload.message_body = xml_file
            result = upload.put()
        hdr = 'Response to UPLOAD for article (iteration {0})'.format(i + 1)
        report(hdr, result)

    for asset_id, asset_filename in case.assets():
        for i in range(2): # First create, then update
            create_asset = asset_req(asset_id)
            create_asset.set_query_parameter('assetOf', case.article_doi())
            with open(asset_filename) as asset_file:
                create_asset.message_body = asset_file
                result = create_asset.put()
            hdr = 'Response to CREATE for asset (iteration {0})'.format(i + 1)
            report(hdr, result)

    read = article_req()
    report('Response to READ', read.get())

    for asset_id, asset_file in case.assets():
        read_asset = asset_req(asset_id)
        report('Response to READ for asset', read_asset.get())
        delete_asset = asset_req(asset_id)
        report('Response to DELETE for asset', delete_asset.delete())

    delete = article_req()
    report('Response to DELETE', delete.delete())


for case in TEST_ARTICLES:
    run_test_on_article(case)
