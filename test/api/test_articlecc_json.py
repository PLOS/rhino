#!/usr/bin/env python2

__author__ = 'fcabrales@plos.org'


"""
This test case validates Rhino's article crud controller.
"""

from ..api.RequestObject.articlecc_json import ArticlesJSON
import resources

class ArticlesTest(ArticlesJSON):

  def setUp(self):
    self.already_done = 0
    print('\nTesting POST zips/\n')
    # Invoke ZIP API
    self.post_ingestible_zip(resources.ZIP_ARTICLE)
    # Validate HTTP code in the response is 201 (CREATED)
    self.verify_http_code_is(resources.CREATED)

  def tearDown(self):
    """
    Purge all records from the db for test article
    """
    if self.already_done > 0: return
    try:
      self.get_article(resources.ARTICLE_DOI)
      if self.get_http_response().status_code == resources.OK:
        self.delete_article_sql_doi(resources.NOT_SCAPE_ARTICLE_DOI)
      else:
        print self.parsed.get_attribute('message')
    except:
      pass


  def test_add_article_revision(self):
    """
    POST revision: Adding article revision to article
    """
    print('\nTesting POST article revision/\n')
    # Invoke article API
    self.add_article_revision(resources.CREATED)
    self.verify_article_revision()

if __name__ == '__main__':
  ArticlesJSON._run_tests_randomly()