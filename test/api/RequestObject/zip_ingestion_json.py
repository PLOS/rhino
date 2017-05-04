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
Base class for Rhino ZIP Ingestion JSON related services
"""

__author__ = 'gfilomeno@plos.org'


from ingestion_json import Ingestion
from ..resources import ZIP_INGESTION_API, ARTICLE_API, COLLECTIONS_API, OBJECTS_API, DEFAULT_HEADERS

class ZIPIngestionJson(Ingestion):

  def post_ingestible_zip(self, archive):
    """
    Calls article API to ingest a zip article file
    POST /zips
    :param archive
    """
    self.doPost(ZIP_INGESTION_API, {'archive': archive})
    self.parse_response_as_json()

  #Article API
  def get_article(self, article_doi=None):
    """
    Calls article API to get an article
    GET /articles/{article_doi}...
    :param article_doi
    """
    self.doGet('%s/%s/%s' % (ARTICLE_API, article_doi, 'ingestions/1'), None, headers=DEFAULT_HEADERS)
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
