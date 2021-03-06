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

import pytest

from .resources import OK, BAD_REQUEST, NOT_FOUND, CREATED
from ..api.RequestObject.articlelistcc import ArticlesListJSON

__author__ = 'fcabrales'


class TestArticlesListAdditions(ArticlesListJSON):

    def lists_cleanup(self):
        """ Cleanup any created list with name starting with 'rhino-cell-collection' """
        self.delete_lists_articlelistjointable('rhino-cell-collection')
        self.delete_lists_articlelist('rhino-cell-collection')

    @pytest.fixture(scope="function", name='cleanup')
    def set_up(self, request):
        self.lists_cleanup()

        def tear_down():
            time.sleep(10)
            self.lists_cleanup()

        request.addfinalizer(tear_down)

    def test_articles_list_addition(self, cleanup):
        logging.info('Adding article list', 'green', attrs=['bold'])
        self.add_article_list(CREATED)

    def test_articles_list_addition_twice(self, cleanup):
        logging.info('Adding two identical article lists', 'green', attrs=['bold'])

        self.add_article_list(CREATED)
        time.sleep(10)
        self.add_article_list(BAD_REQUEST)

    def test_articles_list_patch(self, cleanup):
        logging.info('Patching article list', 'green', attrs=['bold'])

        self.add_article_list(CREATED)
        time.sleep(10)
        self.patch_article_list(OK, "rhino-cell-collection")
        self.patch_article_list(NOT_FOUND, "wombat-cell-collection")
        self.patch_article_list(NOT_FOUND, "rhino-cell-collection", True)  # use bogus data
