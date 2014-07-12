#!/usr/bin/env python2

'''
Base class for Rhino's Articles related service tests.
'''

__author__ = 'jkrzemien@plos.org'

from Base.JSONBasedServiceTest import JSONBasedServiceTest
from Base.Config import RHINO_URL
from Base.Decorators.Api import deduce_doi


class ArticlesBaseTest(JSONBasedServiceTest):

  def __init__(self, module):
    super(ArticlesBaseTest, self).__init__(module)

    self.API_UNDER_TEST = RHINO_URL + '/articles/'

  @deduce_doi
  def updateArticle(self, article, state, syndications=None):
    self.assertIsNotNone(article)
    self.assertIsNotNone(state)

    data = {'state': state }

    if syndications is not None:
      data['syndications'] = syndications

    self.doPatch(self.API_UNDER_TEST + article, data)

