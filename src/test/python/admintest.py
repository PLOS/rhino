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


"""Exercises the ArticleCrudController methods in Rhino.

This script serves as a quick-and-dirty integration test by exercising the
Spring controllers in ways that the unit tests cannot. It requires that a
Tomcat instance be running Rhino at the time the script is executed.

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

class TestVolume(object):
    """One test case of a volume to create."""
    def __init__(self, doi, journal_key, display_name, issues=()):
        self.doi = DOI_PREFIX + doi
        self.journal_key = journal_key
        self.display_name = display_name
        self.issues = issues

    def __str__(self):
        return 'TestVolume({0!r}, {1!r}, {2!r}, {3!r})'.format(
            self.doi, self.journal_key, self.display_name, self.issues)

class TestIssue(object):
    """One test case of an issue to create.

    In order to be created, an instance should belong to the 'issues' field
    of a TestVolume object.
    """
    def __init__(self, suffix, display_name, image_uri=None):
        if not suffix.startswith('.'):
            suffix = '.' + suffix
        self.suffix = suffix
        self.display_name = display_name
        self.image_uri = image_uri

    def __str__(self):
        return 'TestIssue({0!r}, {1!r}, {2!r})'.format(
            self.suffix, self.display_name, self.image_uri)

TEST_VOLUMES = [
    TestVolume('volume.pone.v47', 'PLoSONE', 'TestVolume',
               issues=[TestIssue('i23', 'TestIssue')]),
    ]
"""A list of test volumes to create.

Unlike TEST_ARTICLES, these do not map on to any data outside this script
and can have any values.
"""

class TestArticle(object):
    """One test case of an article to manipulate."""
    def __init__(self, doi, asset_suffixes=()):
        """Create a test case for an article.

        The DOI is the actual DOI for the article; it should not have an
        '.xml' extension. Each asset suffix can be appended to the DOI to
        produce the quasi-DOI identifier of an asset that goes with the
        article. The asset suffixes *should* have filename extensions.
        """
        self.doi = doi
        self.asset_suffixes = asset_suffixes

    def article_doi(self):
        """Return the article's actual DOI."""
        return DOI_PREFIX + self.doi

    def article_id(self):
        """Return the article's RESTful identifier."""
        return self.article_doi()

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
    TestArticle('journal.pone.0038869', ['g001.tif', 'g002.tif']),
    ]

_BANNER_WIDTH = 79

def section(*parts):
    print('=' * _BANNER_WIDTH)
    print(*parts)
    print()

def report(description, response):
    """Print a description of the HTTP response."""
    print('-' * _BANNER_WIDTH)
    print(description)
    print()
    print(response.display())

def build_request(path):
    return Request('localhost', path, port=8080)

def create_test_volume(case):
    """Test volume creation for one case."""
    section('Running volume test for', case)

    req = build_request('volume/' + case.doi)
    req.set_query_parameter('display', case.display_name)
    req.set_query_parameter('journal', case.journal_key)
    result = req.put()
    report('Response to CREATE for volume', result)

    req = build_request('volume/' + case.doi)
    result = req.get()
    report('Response to READ for volume', result)

    for issue_case in case.issues:
        req = build_request('issue/' + case.doi + issue_case.suffix)
        req.set_query_parameter('volume', case.doi)
        req.set_query_parameter('display', issue_case.display_name)
        if issue_case.image_uri:
            req.set_query_parameter('image', issue_case.image_uri)

def run_test_on_article(case):
    """Run the test for one article test case."""
    section('Running article test for', case)

    def article_req(query_param=None):
        url = ['article/', case.article_id()]
        if query_param:
            url += ['?', query_param]
        return build_request(''.join(url))
    def asset_req(asset_id):
        return build_request('asset/' + asset_id)

    for i in range(2): # First create, then update
        upload = build_request('article/')
        with open(case.xml_path()) as xml_file:
            upload.message_body = xml_file
            result = upload.put()
        hdr = 'Response to UPLOAD for article (iteration {0})'.format(i + 1)
        report(hdr, result)

    read_meta = article_req('format=json')
    report('Response to READ (article metadata, no assets)', read_meta.get())

    read_data = article_req()
    report('Response to READ (article XML)', read_data.get())

    def upload_asset(description, asset_id, asset_filename, article_id):
        req = asset_req(asset_id)
        if article_id:
            req.set_query_parameter('assetOf', article_id)
        with open(asset_filename) as asset_file:
            req.message_body = asset_file
            result = req.put()
        report(description, result)

    for asset_id, asset_filename in case.assets():
        continue
        upload_asset('Response to creating asset',
                     asset_id, asset_filename, case.article_doi())
        upload_asset('Response to re-uploading asset',
                     asset_id, asset_filename, case.article_doi())
        upload_asset('Response to re-uploading asset without article DOI',
                     asset_id, asset_filename, None)

    report('Response to READ (article metadata, with assets)', read_meta.get())

    for asset_id, asset_file in case.assets():
        continue
        read_asset = asset_req(asset_id)
        report('Response to READ for asset', read_asset.get())
        delete_asset = asset_req(asset_id)
        report('Response to DELETE for asset', delete_asset.delete())

    delete = article_req()
    report('Response to DELETE', delete.delete())


for volume_case in TEST_VOLUMES:
    create_test_volume(volume_case)
for article_case in TEST_ARTICLES:
    run_test_on_article(article_case)
