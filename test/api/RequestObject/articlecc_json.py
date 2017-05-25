#!/usr/bin/env python2

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
Base class for Article crud controller JSON related services
"""

__author__ = 'fcabrales@plos.org'

from .zip_ingestion_json import ZIPIngestionJson
from ..resources import *
from ...Base.api import needs
from ...Base.MySQL import MySQL
from ..resources import NOT_SCAPE_ARTICLE_DOI

ARTICLE_API = API_BASE_URL + '/articles/'
ARTICLE_REVISION_API = ARTICLE_API + ARTICLE_DOI + '/revisions'


class ArticlesJSON(ZIPIngestionJson):

  @needs('parsed', 'parse_response_as_json()')
  def verify_article_revision(self):
    """
    Validate setting article revision using articleRevision table
    """
    db_article_revision = self.get_article_sql_revision(NOT_SCAPE_ARTICLE_DOI)
    self.get_article_revisions()
    self.verify_article_revision_db_expected(db_article_revision[0], 'revisionNumber')

  def add_article_revision(self, expected_response_code):
    """
    Calls article API to write revision for an article
    POST /articles/{doi}/revisions
    :param doi
    :param revision
    :param ingestion
    """
    self.doPost('%s?revision=%s&ingestion=%s' % (ARTICLE_REVISION_API,REVISION, INGESTION_NUMBER))
    self.verify_http_code_is(expected_response_code)


  """
  Below SQL statements will query ambra articleRevision table for revision number by articleDoi
  """
  def get_article_sql_revision (self,article_doi):
    article_revision = MySQL().query('SELECT ar.revisionNumber FROM articleRevision as ar JOIN articleIngestion ai ON '
                                     'ar.ingestionId = ai.ingestionId JOIN article a ON ai.articleId = a.articleId '
                                     'where a.doi = %s', [article_doi])
    return article_revision[0]


  #Article API
  def get_article_revisions(self):
    """
    Calls article API to get an article revisions
    GET /articles/{article_doi}/revisions
    :param article_doi example:10.1371%2B%2Bjournal.pone.0155391
    """
    self.doGet(ARTICLE_REVISION_API, None, headers=DEFAULT_HEADERS)
    self.parse_response_as_json()

  @needs('parsed', 'parse_response_as_json()')
  def verify_article_revision_db_expected(self, expected_results, attribute):
    actual_results = self.parsed.get_article_revision_number()
    assert actual_results[0] == expected_results, \
      ('%s is not correct! actual: %s expected: %s' % (attribute, actual_results[0], expected_results))
