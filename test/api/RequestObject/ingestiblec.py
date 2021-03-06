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
Base class for Rhino's Ingest API service tests.
"""

from test.api import resources

__author__ = 'fcabrales@plos.org'

from ...Base.base_service_test import BaseServiceTest
from ..resources import ARTICLE_API, DEFAULT_HEADERS


class IngestibleJSON(BaseServiceTest):

    def get_ingestible(self, article_doi=None):
        """
        Calls ingestible API to get a repack article
        GET /articles/{article_doi}/ingestions/{ingestion_number}/ingestible
        :param article_doi
        """
        response = self.doGet('{0}/{1}/ingestions/{2!s}/ingestible'.format(ARTICLE_API, article_doi,
                                         self.ingestion_number), None, headers=DEFAULT_HEADERS)
        return response
