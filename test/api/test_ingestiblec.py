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

__author__ = 'fcabrales@plos.org'


"""
This test case validates Rhino's article repack service.
"""

from .RequestObject.articlecc import ArticlesJSON
from .RequestObject.ingestiblec import IngestibleJSON
from .RequestObject.memory_zip import MemoryZipJSON
from test.api import resources

class ZipIngestibleTest(IngestibleJSON, MemoryZipJSON, ArticlesJSON):

  def setUp(self):
    print('\nTesting POST zips/\n')
    # Invoke ZIP API
    zip_file = self.create_ingestible(resources.RA_DOI, 'RelatedArticle/')
    self.post_ingestible_zip(zip_file)
    # Validate HTTP code in the response is 201 (CREATED)
    self.verify_http_code_is(resources.CREATED)

  def tearDown(self):
    """
    Purge all objects and collections created in the test case
    """
    #self.delete_test_article()

  def test_ingestible(self):
    """
    GET ingestibles
    """
    print('\nTesting GET ingestibles/\n')
    # Invoke ingestibles API
    self.get_ingestible(resources.RELATED_ARTICLE_DOI)
    self.verify_http_code_is(resources.OK)
    self.delete_test_article(resources.RELATED_ARTICLE_DOI, resources.NOT_SCAPE_RELATED_ARTICLE_DOI,
                             resources.RELATED_ARTICLE_BUCKET_NAME)

  def delete_test_article(self, article_doi, not_scaped_article_doi, bucket_name):
    """
    Gets article information for rhino then proceeds to delete article records from ambra db
    and content repo database
    :param article_doi: String. Such as '10.1371++journal.pone.0170224'  
    :param not_scapted_article_doi: String. Such as '10.1371/journal.pone.0155391'
    :param bucket_name: String. Such as 'preprint'
    :return: None
    """
    try:
      self.get_article(article_doi)
      status_code = self.get_http_response().status_code
      if status_code == resources.OK:
        self.delete_article_sql_doi(not_scaped_article_doi)
        #Delete article
        self.delete_article(article_doi)
        self.verify_http_code_is(resources.NOT_FOUND)
        #Delete CRepo collections
        self.delete_test_collections(article_doi,bucket_name)
        #Delete CRepo objects
        self.delete_test_objects(article_doi,bucket_name)
    except AssertionError:
      print ('HTTP response code assertion error during article db deletion')
    return status_code

  def delete_test_collections(self, article_doi, bucket_name):
    """
    Get collection information from content repo using bucket name and article doi, then
    proceeded to call content repo delete collection endpoint
    :param article_doi: String. Such as '10.1371++journal.pone.0170224'  
    :param bucket_name: String. Such as 'preprint'
    :return: None
    """
    self.get_collection_versions(bucketName=bucket_name, key=article_doi)
    collections = self.parsed.get_list()
    if collections:
      for coll in collections:
        self.delete_collection(bucketName=bucket_name, key=coll['key'], version=coll['versionNumber'])
        self.verify_http_code_is(resources.OK)

  def delete_test_objects(self,  article_doi, bucket_name):
    """
    Get object information from content repo using bucket name, then
    proceeded to call content repo delete object endpoint
    :param article_doi: String. Such as '10.1371++journal.pone.0170224'  
    :param bucket_name: String. Such as 'preprint'
    :return: None
    """
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
  IngestibleJSON.run_tests_randomly()