#!/usr/bin/env python2

"""
Base class for Rhino ZIP Ingestion JSON related services
"""

__author__ = 'gfilomeno@plos.org'

from ...Base.Config import API_BASE_URL
from ingestio_json import Ingestion

DEFAULT_HEADERS = {'Accept': 'application/json'}

ZIP_INGESTION_API = API_BASE_URL + '/zips'
ARTICLE_API = API_BASE_URL + '/articles'

class ZIPIngestionJson(Ingestion):

  def post_ingestible_zip(self, archive, force_reingest=''):
    self.doPost(ZIP_INGESTION_API, {"force_reingest": force_reingest}, {'archive': open(self.find_file(archive), 'rb')})
    self.parse_response_as_json()

  def get_article(self, article_doi=None):
    self.doGet('%s/%s' % (ARTICLE_API, article_doi), None, headers=DEFAULT_HEADERS)
    self.parse_response_as_json()

  def delete_article(self, article_doi=None):
    self.doDelete('%s/%s' % (ARTICLE_API, article_doi), None, headers=DEFAULT_HEADERS)

  def unpublish_article(self, article_doi=None, **kwargs):
    self.doPatch('%s/%s' % (ARTICLE_API, article_doi), data=kwargs, headers=DEFAULT_HEADERS)
