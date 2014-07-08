#!/usr/bin/env python2

'''
Base class for Rhino's Articles related service tests.
'''

__author__ = 'jkrzemien@plos.org'

import requests
from Base.BaseServiceTest import BaseServiceTest
from Base.Config import RHINO_URL
from datetime import datetime
import json
from Base.Decorators import ensure_api_called, timeit


class ArticlesBaseTest(BaseServiceTest):

  def __init__(self, module):
    super(ArticlesBaseTest, self).__init__(module)

    self.API_UNDER_TEST = RHINO_URL + '/articles/10.1371/journal.'

  @timeit
  def updateArticle(self, article, state, syndications=None):
    assert article is not None
    assert state is not None

    daData = {'state': state }

    if syndications is not None:
      daData['syndications'] = syndications

    self._testStartTime = datetime.now()
    self._response = requests.patch(self.API_UNDER_TEST + article, data=json.dumps(daData), verify=False)
    self._apiTime = (datetime.now() - self._testStartTime).total_seconds()

    print self._response
    #self._jsonResponse = self.get_response_as_json()
    self._textResponse = self.get_response_as_text()
    print self._textResponse
