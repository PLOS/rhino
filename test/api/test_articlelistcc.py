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
This test cases validates JSON article list crud controller.
"""

import logging
import time
from termcolor import cprint

from ..api.RequestObject.articlelistcc import ArticlesListJSON
from .resources import OK, BAD_REQUEST, NOT_FOUND, CREATED

__author__ = 'fcabrales'


class ArticlesListAdditions(ArticlesListJSON):

    def test_cleanup(self):
        """
        Cleanup any created list with name starting with "rhino-cell-collection"
        """
        self.delete_lists_articlelistjointable('rhino-cell-collection')
        self.delete_lists_articlelist('rhino-cell-collection')

    def test_articles_list_addition(self):
        self.test_cleanup()
        logging.info('Adding article list', 'green', attrs=['bold'])

        self.add_article_list(CREATED)
        time.sleep(10)
        self.test_cleanup()

    def test_articles_list_addition_twice(self):
        self.test_cleanup()
        logging.info('Adding two identical article lists', 'green', attrs=['bold'])

        self.add_article_list(CREATED)
        time.sleep(10)
        self.add_article_list(BAD_REQUEST)
        time.sleep(5)
        self.test_cleanup()

    def test_article_list_patch(self):
        self.test_cleanup()
        logging.info('Patching article list', 'green', attrs=['bold'])

        self.add_article_list(CREATED)
        time.sleep(10)
        self.patch_article_list(OK, "rhino-cell-collection")
        self.patch_article_list(NOT_FOUND, "wombat-cell-collection")
        self.patch_article_list(NOT_FOUND, "rhino-cell-collection", True)  # use bogus data
        time.sleep(5)
        self.test_cleanup()


if __name__ == '__main__':
    ArticlesListJSON.run_tests_randomly()
