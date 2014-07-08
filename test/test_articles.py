#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

'''
This test cases validates Rhino's Articles API.
'''

from Base.ArticlesBaseTest import ArticlesBaseTest
#from Base.Database import Database


class ArticlesTest(ArticlesBaseTest):

  def test_article_syndication_happy_path(self):
    syndications = {
      'CROSSREF': { 'status': 'IN_PROGRESS'}
    }

    self.updateArticle('pone.0097823', 'published', syndications)
    self.verify_HTTP_code_is(200)


if __name__ == '__main__':
  ArticlesBaseTest._run_tests_randomly()