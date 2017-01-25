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
