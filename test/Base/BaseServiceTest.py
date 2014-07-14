#!/usr/bin/env python2

'''
Base class for Rhino related service tests.
'''

__author__ = 'jkrzemien@plos.org'

import unittest
import random
from Base.Decorators.Api import ensure_api_called, timeit
from Config import TIMEOUT, PRINT_DEBUG
import requests
import json


class BaseServiceTest(unittest.TestCase):

  def __init__(self, module):
    super(BaseServiceTest, self).__init__(module)
    self._response = None
    self._testStartTime = None
    self._apiTime = None

  def setUp(self):
    self._response = None
    self._testStartTime = None
    self._apiTime = None

  def tearDown(self):
    self._response = None
    self._testStartTime = None
    self._apiTime = None

  @timeit
  def doGet(self, url, params=None):
    self._response = requests.get(url, params=params, verify=False, timeout=TIMEOUT,
      allow_redirects=True)
    if PRINT_DEBUG:
      print self._response.text

  @timeit
  def doPost(self, url, data=None, files=None):
    self._response = requests.post(url, data=data, files=files, verify=False, timeout=TIMEOUT,
      allow_redirects=True)
    if PRINT_DEBUG:
      print self._response.text

  @timeit
  def doPatch(self, url, data=None):
    self._response = requests.patch(url, data=json.dumps(data), verify=False, timeout=TIMEOUT,
      allow_redirects=True)
    if PRINT_DEBUG:
      print self._response.text

  def doDelete(self, url, data=None):
    pass

  def doUpdate(self, url, data=None):
    pass

  def get_response(self):
    return self._response

  @ensure_api_called
  def get_response_as_text(self):
    return self.get_response().text

  @ensure_api_called
  def get_response_as_binary(self):
    return self.get_response().content

  @ensure_api_called
  def verify_HTTP_code_is(self, httpCode):
    print 'Validating HTTP response code to be %s...' % httpCode,
    self.assertEqual(self._response.status_code, httpCode)
    print 'OK'

  @staticmethod
  def _run_tests_randomly():
    unittest.TestLoader.sortTestMethodsUsing = lambda _, x, y: random.choice([-1, 1])
    unittest.main()