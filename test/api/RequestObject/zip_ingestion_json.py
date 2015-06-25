#!/usr/bin/env python2

"""
Base class for Rhino ZIP Ingestion JSON related services
"""

__author__ = 'gfilomeno@plos.org'


from ingestio_json import Ingestion
from ..resources import *

class ZIPIngestionJson(Ingestion):

  def post_ingestible_zip(self, archive, force_reingest=None):
    """
    Calls article API to ingest a zip article file
    POST /zips
    :param archive, force_reingest
    """
    self.doPost(ZIP_INGESTION_API, {"force_reingest": force_reingest}, {'archive': open(self.find_file(archive), 'rb')})
    self.parse_response_as_json()

  #Article API
  def get_article(self, article_doi=None):
    """
    Calls article API to get an article
    GET /articles/{article_doi}...
    :param article_doi
    """
    self.doGet('%s/%s' % (ARTICLE_API, article_doi), None, headers=DEFAULT_HEADERS)
    self.parse_response_as_json()

  def delete_article(self, article_doi=None):
    """
    Calls article API to delete an article
    DELETE /articles/{article_doi}...
    :param article_doi
    """
    self.doDelete('%s/%s' % (ARTICLE_API, article_doi), None, headers=DEFAULT_HEADERS)

  #Content Repo API
  def get_collection_versions(self, bucketName=None, **kwargs):
    """
    Calls CREPO API to get a collection versions
    :param name: bucket name, key
    """
    self.doGet('%s/versions/%s' % (COLLECTIONS_API, bucketName), params=kwargs, headers=DEFAULT_HEADERS)
    self.parse_response_as_json()

  def delete_collection(self, bucketName=None, **kwargs):
    """
    Calls CREPO API to delete a collection
    :param name: bucket name.
    """
    self.doDelete('%s/%s' % (COLLECTIONS_API, bucketName), params=kwargs, headers=DEFAULT_HEADERS)
    if self.get_http_response().status_code != OK:
      self.parse_response_as_json()

  def get_crepo_objets(self, bucketName=None, **kwargs):
    """
    Calls CREPO API to get objects list in a bucket
    GET /objects?bucketName={bucketName}...
    :param bucketName, offset, limit, includeDeleted, includePurged, tag
    """
    self.doGet('%s?bucketName=%s' % (OBJECTS_API, bucketName), kwargs, DEFAULT_HEADERS)
    self.parse_response_as_json()

  def delete_object(self, bucketName=None, **kwargs):
    """
    Calls CREPO API to delete a object
    :param name: bucket name.
    """
    self.doDelete('%s/%s' % (OBJECTS_API, bucketName), params=kwargs, headers=DEFAULT_HEADERS)