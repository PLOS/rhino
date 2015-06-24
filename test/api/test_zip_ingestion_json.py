#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org; gfilomeno@plos.org'


"""
This test case validates Rhino's convenience zipUpload Tests for ZIP ingestion.
"""

from ..api.RequestObject.zip_ingestion_json import ZIPIngestionJson
import resources

ARTICLE_DOI =  '10.1371/journal.pone.0097823'

class ZipIngestionTest(ZIPIngestionJson):

  def setUp(self):
    self.already_done = 0


  def tearDown(self):
    """
    Purge all objects and collections created in the test case
    """
    if self.already_done > 0: return
    self.get_article(ARTICLE_DOI)
    if self.get_http_response().status_code == resources.OK:
      self.delete_article(ARTICLE_DOI)
      self.verify_http_code_is(resources.OK)
    else:
      print self.parsed.get_attribute('message')



  def test_zip_ingestion(self):
    """
    POST zips: Forced ingestion of ZIP archive
    """
    # Invoke ZIP API
    self.post_ingestible_zip(resources.zip_article, force_reingest=True)
    # Validate HTTP code in the response is 201 (CREATED)
    self.verify_http_code_is(resources.CREATED)
    # Validate response with Article table
    self.verify_article()
    # Validate response with Syndication table
    self.verify_syndications()
    # Validate response with Journal table
    self.verify_journals()
    # Validate response with CitedArticle and CitedPerson tables
    self.verify_citedArticles()
    # Validate response with ArticleAsset table
    self.verify_article_file(resources.PDF_CONTENT_TYPE, 'articlePdf')
    self.verify_article_file(resources.XML_CONTENT_TYPE, 'articleXml')
    self.verify_article_figures()
    self.verify_article_graphics()

  def test_zip_ingestion_force_false(self):
    """
    POST zips: Not forced ingestion of ZIP archive
    """
    # Ingest a ZIP file
    self.post_ingestible_zip(resources.zip_article, force_reingest=True)
    # Validate HTTP code in the response is 201 (CREATED)
    self.verify_http_code_is(resources.CREATED)
    # Try to ingest the same ZIP file for a second time
    try:
      self.post_ingestible_zip(resources.zip_article, force_reingest=None)
      self.fail('No JSON object could be decoded')
    except:
      pass

if __name__ == '__main__':
  ZIPIngestionJson._run_tests_randomly()