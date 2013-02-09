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


DOI_PREFIX = '10.1371/journal.'
TEST_DATA_PATH = '../resources/articles/'

class ArticleCase(object):
    """One test case of an article to manipulate."""
    def __init__(self, path, doi, asset_suffixes=()):
        """Create a test case for an article.

        The DOI is the actual DOI for the article; it should not have an
        '.xml' extension.

        Each asset suffix is a pair containing the DOI suffix (article DOI
        + suffix == asset DOI) and the asset's file extension.
        """
        self.path = path
        self.doi = doi
        self.assets = [AssetCase(self, suffix, extension)
                       for suffix, extension in asset_suffixes]

    def article_doi(self):
        """Return the article's actual DOI."""
        return DOI_PREFIX + self.doi

    def xml_path(self):
        """Return a local file path from this script to the article's data."""
        return os.path.join(self.path, self.doi + '.xml')

    def __str__(self):
        return 'TestArticle({0!r}, {1!r})'.format(self.doi, self.asset_suffixes)

class AssetCase(object):
    """One test case of an asset file to upload."""
    def __init__(self, article, suffix, extension):
        self.article = article
        self.suffix = suffix
        self.extension = extension

    def path(self):
        """Return a local file path from this script to the asset file."""
        filename = '.'.join([self.article.doi, self.suffix, self.extension])
        return os.path.join(self.article.path, filename)

    def doi(self):
        """Return the asset's DOI."""
        return '.'.join([self.article.article_doi(), self.suffix])

    def brief_name(self):
        """Return an abbreviated name for this asset.

        It does not repeat the parent article's DOI.
        """
        return '.'.join([self.suffix, self.extension])
