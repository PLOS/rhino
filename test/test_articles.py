#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

'''
This test cases validates Rhino's Articles API.
'''

from Base.ArticlesBaseTest import ArticlesBaseTest


class ArticlesTest(ArticlesBaseTest):

  def test_article_syndication_happy_path(self):

    desiredState = 'published'

    syndications = {
      'CROSSREF': { 'status': 'IN_PROGRESS'}
    }

    self.updateArticle('10.1371/journal.pone.0097823', desiredState, syndications)
    self.verify_HTTP_code_is(200)
    self.verify_state_is('published')
    #self.verify_doi_is_correct()
    #self.verify_graphics_section()


if __name__ == '__main__':
  ArticlesBaseTest._run_tests_randomly()