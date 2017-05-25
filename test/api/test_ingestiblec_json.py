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

from ..api.RequestObject.ingestiblec_json import IngestibleJSON
from ..api.RequestObject.memory_zip_json import MemoryZipJSON
from ..api.RequestObject.articlecc_json import ArticlesJSON
import resources

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
    :param None
    :return: None
    """
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
    """
    Get collection information from content repo using bucket name and article doi, then
    proceeded to call content repo delete collection endpoint
    :param None
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
    :param None
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
  IngestibleJSON._run_tests_randomly()
