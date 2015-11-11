__author__ = 'fcabrales'

'''
This test cases validates JSON article list crud controller.
'''

from test.api.RequestObject.articlelistcc_json import ArticlesListJSON
import time
from termcolor import cprint

class ArticlesListAdditions(ArticlesListJSON):

  def test_cleanup(self):
    """
    Cleanup any created list with name starting with "rhino-cell-collection"
    """
    self.delete_lists_articlelistjointable('rhino-cell-collection')
    self.delete_lists_articlelist('rhino-cell-collection')

  def test_articles_list_addition(self):
    cprint('Adding article list', 'green', attrs=['bold'])

    """
    add article list API call with %articlelistbody
    """
    expected_response_code = 201
    self.add_article_list(expected_response_code)
    time.sleep(10)
    self.test_cleanup()

  def test_articles_list_addition_twice(self):
    cprint('Adding two identical article lists', 'green', attrs=['bold'])
    """
    add article list API call with %articlelistbody
    """
    expected_response_code = 201
    self.add_article_list(expected_response_code)
    time.sleep(10)
    expected_response_code = 400
    self.add_article_list(expected_response_code)
    time.sleep(5)
    self.test_cleanup()

if __name__ == '__main__':
    ArticlesListJSON._run_tests_randomly()