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

import os


"""Test case representations for articles and assets."""


DOI_PREFIX = '10.1371/'
TEST_DATA_PATH = '../resources/articles/'

class ArticleCase(object):
    """One test case of an article to manipulate."""
    def __init__(self, path, doi, asset_suffixes=()):
        """Create a test case for an article.

        The DOI is the actual DOI for the article; it should not have an
        '.xml' extension. Each asset suffix can be appended to the DOI to
        produce the quasi-DOI identifier of an asset that goes with the
        article. The asset suffixes *should* have filename extensions.
        """
        self.path = path
        self.doi = doi
        self.asset_suffixes = asset_suffixes

    def article_doi(self):
        """Return the article's actual DOI."""
        return DOI_PREFIX + self.doi

    def xml_path(self):
        """Return a local file path from this script to the article's data."""
        return os.path.join(self.path, self.doi + '.xml')

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
