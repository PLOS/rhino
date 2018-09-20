#!/usr/bin/env python3
# -*- coding: utf-8 -*-

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


"""
This test case validates Rhino's article crud controller.
"""

import logging
from ddt import ddt, data, unpack

from .RequestObject.articlecc import ArticlesJSON
from .RequestObject.memory_zip import MemoryZipJSON
from ..api import resources

CROSSFER = 'crossref'
JISC = 'jisc'
PMC = 'pmc'
PUBMED = 'pubmed'
FIGSHARE = 'figshare'

__author__ = 'fcabrales@plos.org'

@ddt
class ArticlesTest(ArticlesJSON, MemoryZipJSON):

    def setUp(self):
        logging.info('\nTesting POST zips/\n')
        # Invoke ZIP API to generate in memory ingestible zip
        zip_file = self.create_ingestible(resources.RA_DOI, 'RelatedArticle/')
        response = self.post_ingestible_zip(zip_file, resources.RELATED_ARTICLE_BUCKET_NAME)
        # Validate HTTP code in the response is 201 (CREATED)
        self.verify_http_code_is(response, resources.CREATED)

    def tearDown(self):
        """
        Purge all records from the db for test article
        """
        try:
            response = self.get_article(resources.RELATED_ARTICLE_DOI)
            if response.raise_for_status() is None:
                self.delete_article_sql_doi(resources.NOT_SCAPE_RELATED_ARTICLE_DOI)
            else:
                logging.info(self.parsed.get_attribute('message'))
        except:
            pass

    def test_add_article_revision(self):
        """
        POST revision: Adding article revision to article
        """
        logging.info('\nTesting POST article revision/\n')
        # Invoke article API
        self.add_article_revision(resources.CREATED)
        self.verify_article_revision()

    @data(
        CROSSFER,
        JISC,
        PMC,
        PUBMED,
        FIGSHARE,
    )
    def test_add_article_syndication_crossref(self, syndication_target):
        """
        POST revision: Adding article syndication to article
        """
        logging.info('\nTesting POST article syndication/\n')
        # Invoke article syndication API call
        self.add_article_syndication(resources.CREATED, syndication_target)

if __name__ == '__main__':
    ArticlesJSON.run_tests_randomly()
