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

from .RequestObject.zip_ingestion_json import ZIPIngestionJson
from .RequestObject.memory_zip_json import MemoryZipJSON
from test.api import resources

class ZipIngestionTest(ZIPIngestionJson, MemoryZipJSON):

  def setUp(self):
    self.already_done = 0

  def tearDown(self):
    """
    Purge all objects and collections created in the test case
    """
    if self.already_done > 0: return
    # Delete article and crepo objects
    self.delete_test_article()

  def test_zip_ingestion(self):
    """
    POST zips: Forced ingestion of ZIP archive
    """
    print('\nTesting POST zips/\n')
    # Invoke ZIP API
    zip_file = self.create_ingestible()
    self.post_ingestible_zip(zip_file)
    # Validate HTTP code in the response is 201 (CREATED)
    self.verify_http_code_is(resources.CREATED)
    # Validate response with database tables
    self.verify_zip_ingestion()

  def test_zip_ingestion_without_file(self):
    """
    POST zips: Try to ingest of ZIP archive without file name
    """
    print('\nTesting POST zips/ without parameters\n')
    # Ingest a ZIP file
    try:
      self.already_done = 1
      self.post_ingestible_zip(None)
    except:
      pass

  def verify_zip_ingestion(self):
    # All below verifications will be fix with https://developer.plos.org/jira/browse/DPRO-3259
    # Validate response with Article table
    self.verify_article()
    # Validate response with Journal table
    self.verify_journals()
    # Validate article figures
    self.verify_article_figures()

  def delete_test_article(self):
    try:
      self.get_article(resources.ARTICLE_DOI)
      if self.get_http_response().status_code == resources.OK:
        self.delete_article_sql_doi(resources.NOT_SCAPE_ARTICLE_DOI)
        #Delete article
        self.delete_article(resources.ARTICLE_DOI)
        self.verify_http_code_is(resources.OK)
        #Delete CRepo collections
        self.delete_test_collections()
        #Delete CRepo objects
        self.delete_test_objects()

      else:
        print(self.parsed.get_attribute('message'))
    except:
      pass

  def delete_test_collections(self):
    self.get_collection_versions(bucketName=resources.BUCKET_NAME, key=resources.ARTICLE_DOI)
    collections = self.parsed.get_list()
    if collections:
      for coll in collections:
        self.delete_collection(bucketName=resources.BUCKET_NAME, key=coll['key'], version=coll['versionNumber'])
        self.verify_http_code_is(resources.OK)


  def delete_test_objects(self):
    self.get_crepo_objets(bucketName=resources.BUCKET_NAME)
    self.verify_http_code_is(resources.OK)
    objects = self.parsed.get_list()
    if objects:
      for object in objects:
        if resources.ARTICLE_DOI in object['key']:
          self.delete_object(bucketName=resources.BUCKET_NAME, key=object['key'], version=object['versionNumber'],
                             purge=True)
          self.verify_http_code_is(resources.OK)

if __name__ == '__main__':
  ZIPIngestionJson._run_tests_randomly()
