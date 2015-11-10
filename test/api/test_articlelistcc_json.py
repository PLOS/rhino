__author__ = 'fcabrales'

'''
This test cases validates JSON article list crud controller.
'''

from test.api.RequestObject.articlelistcc_json import ArticlesListJSON
import time

class ArticlesListAdditions(ArticlesListJSON):

  def test_cleanup(self):
    """
    Cleanup any created list with name starting with "rhino-cell-collection"
    """
    self.delete_lists_articlelistjointable('rhino-cell-collection')
    self.delete_lists_articlelist('rhino-cell-collection')

  def test_articles_list_addition(self):
    """
    add article list API call with %articlelistbody
    """
    self.add_article_list()
    time.sleep(10)
    self.delete_lists_articlelistjointable('rhino-cell-collection')
    self.delete_lists_articlelist('rhino-cell-collection')

  def test_articles_list_addition_twice(self):
    """
    add article list API call with %articlelistbody
    """
    self.add_article_list()
    time.sleep(10)
    self.add_article_list()
    time.sleep(5)
    self.delete_lists_articlelistjointable('rhino-cell-collection')
    self.delete_lists_articlelist('rhino-cell-collection')

if __name__ == '__main__':
    ArticlesListJSON._run_tests_randomly()