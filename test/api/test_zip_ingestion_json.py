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

__author__ = 'jkrzemien@plos.org; gfilomeno@plos.org'


"""
This test case validates Rhino's convenience zipUpload Tests for ZIP ingestion.
"""

from ..api.RequestObject.zip_ingestion_json import ZIPIngestionJson
from ..api.RequestObject.memory_zip_json import MemoryZipJSON
import resources

class ZipIngestionTest(ZIPIngestionJson, MemoryZipJSON):

  def setUp(self):
    self.already_done = 0

  def tearDown(self):
    """
    Purge all objects and collections created in the test case
    """
    if self.already_done > 0: return

  def test_zip_ingestion_related_article(self):
    """
    POST zips: Forced ingestion of ZIP archive
    """
    print('\nTesting POST zips for related article/\n')
    # Invoke ZIP API
    zip_file = self.create_ingestible(resources.RA_DOI, 'RelatedArticle/')
    self.post_ingestible_zip(zip_file, resources.RELATED_ARTICLE_BUCKET_NAME)
    # Validate HTTP code in the response is 201 (CREATED)
    self.verify_http_code_is(resources.CREATED)
    # Validate response with database tables
    self.verify_zip_ingestion(resources.NOT_SCAPE_RELATED_ARTICLE_DOI)
    self.delete_test_article(resources.RELATED_ARTICLE_DOI, resources.NOT_SCAPE_RELATED_ARTICLE_DOI,
                             resources.RELATED_ARTICLE_BUCKET_NAME)

  # def test_zip_ingestion_related_article_no_bucket(self):
  #   """
  #   POST zips: Forced ingestion of ZIP archive
  #   """
  #   print('\nTesting POST zips for related article no bucket/\n')
  #   # Invoke ZIP API
  #   zip_file = self.create_ingestible(resources.RA_DOI)
  #   self.post_ingestible_zip(zip_file)
  #   # Validate HTTP code in the response is 201 (CREATED)
  #   self.verify_http_code_is(resources.CREATED)
  #   # Validate response with database tables
  #   self.verify_zip_ingestion(resources.NOT_SCAPE_RELATED_ARTICLE_DOI)
  #   self.delete_test_article(resources.RELATED_ARTICLE_DOI, resources.NOT_SCAPE_RELATED_ARTICLE_DOI,
  #                            resources.RELATED_ARTICLE_BUCKET_NAME)

  # def test_zip_ingestion_preprint_article(self):
  #   """
  #   POST zips: Forced ingestion of ZIP archive
  #   """
  #   print('\nTesting POST zips for preprint article/\n')
  #   # Invoke ZIP API
  #   zip_file = self.create_ingestible(resources.PP_DOI, 'PrePrint/')
  #   self.post_ingestible_zip(zip_file, resources.PREPRINT_ARTICLE_BUCKET_NAME)
  #   # Validate HTTP code in the response is 201 (CREATED)
  #   self.verify_http_code_is(resources.CREATED)
  #   # Validate response with database tables
  #   self.verify_zip_ingestion(resources.NOT_SCAPE_PREPRINT_ARTICLE_DOI)
  #   self.delete_test_article(resources.PREPRINT_ARTICLE_DOI, resources.NOT_SCAPE_PREPRINT_ARTICLE_DOI,
  #                            resources.PREPRINT_ARTICLE_BUCKET_NAME)

  # def test_zip_ingestion_without_file(self):
  #   """
  #   POST zips: Try to ingest of ZIP archive without file name
  #   """
  #   print('\nTesting POST zips/ without parameters\n')
  #   # Ingest a ZIP file
  #   try:
  #     self.already_done = 1
  #     self.post_ingestible_zip(None)
  #   except:
  #     pass

  def verify_zip_ingestion(self, not_scaped_article_doi):
    # All below verifications will be fix with https://developer.plos.org/jira/browse/DPRO-3259
    # Validate response with Article table
    self.verify_article(not_scaped_article_doi)
    # Validate response with Journal table
    self.verify_journals(not_scaped_article_doi)
    # Validate article figures
    self.verify_article_figures(not_scaped_article_doi)

  def delete_test_article(self, article_doi, not_scaped_article_doi, bucket_name):
    try:
      self.get_article(article_doi)
      if self.get_http_response().status_code == resources.OK:
        self.delete_article_sql_doi(not_scaped_article_doi)
        #Delete article
        self.delete_article(article_doi)
        self.verify_http_code_is(resources.OK)
        #Delete CRepo collections
        self.delete_test_collections(article_doi,bucket_name)
        #Delete CRepo objects
        self.delete_test_objects(article_doi,bucket_name)

      else:
        print self.parsed.get_attribute('message')
    except:
      pass

  def delete_test_collections(self, article_doi, bucket_name):
    self.get_collection_versions(bucketName=bucket_name, key=article_doi)
    collections = self.parsed.get_list()
    if collections:
      for coll in collections:
        self.delete_collection(bucketName=bucket_name, key=coll['key'], version=coll['versionNumber'])
        self.verify_http_code_is(resources.OK)

  def delete_test_objects(self,  article_doi, bucket_name):
    self.get_crepo_objets(bucketName=bucket_name)
    self.verify_http_code_is(resources.OK)
    objects = self.parsed.get_list()
    if objects:
      for object in objects:
        if article_doi in object['key']:
          self.delete_object(bucketName=bucket_name, key=object['key'], version=object['versionNumber'],
                             purge=True)
          self.verify_http_code_is(resources.OK)

if __name__ == '__main__':
  ZIPIngestionJson._run_tests_randomly()
