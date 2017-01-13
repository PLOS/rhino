#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org; gfilomeno@plos.org'


"""
This test case validates Rhino's convenience zipUpload Tests for ZIP ingestion.
"""

from ..api.RequestObject.zip_ingestion_json import ZIPIngestionJson
import resources

class ZipIngestionTest(ZIPIngestionJson):

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
    self.post_ingestible_zip(resources.ZIP_ARTICLE)
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
      self.post_ingestible_zip(None)
    except:
      pass

  def verify_zip_ingestion(self):
    # All below verifications will be fix with https://developer.plos.org/jira/browse/DPRO-3259
    # Validate response with Article table
    self.verify_article()
    # # Validate response with Syndication table
    # self.verify_syndications(resources.ZIP_ARTICLE)
    # # Validate response with Journal table
    self.verify_journals()
    # # Validate response with CitedArticle and CitedPerson tables
    # self.verify_citedArticles(resources.ARTICLE_ID)
    # # Validate response with ArticleAsset table
    # self.verify_article_file(resources.ARTICLE_ID, resources.PDF_CONTENT_TYPE, 'articlePdf')
    # self.verify_article_file(resources.ZIP_ARTICLE, resources.XML_CONTENT_TYPE, 'articleXml')
    # self.verify_article_figures()
    # self.verify_article_graphics(resources.ARTICLE_ID)

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
        print self.parsed.get_attribute('message')
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