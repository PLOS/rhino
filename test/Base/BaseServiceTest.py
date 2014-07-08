#!/usr/bin/env python2

'''
Base class for Rhino related service tests.
'''

__author__ = 'jkrzemien@plos.org'

import unittest
import random
from os import path
from Base.Decorators import ensure_api_called


class BaseServiceTest(unittest.TestCase):

  DOI_PREFFIX = 'info:doi/10.1371/journal.'

  def __init__(self, module):
    super(BaseServiceTest, self).__init__(module)
    self._response = None

  def setUp(self):
    self._response = None

  def tearDown(self):
    self._response = None

  def get_response(self):
    return self._response

  @ensure_api_called
  def get_response_as_text(self):
    return self.get_response().text

  @ensure_api_called
  def get_response_as_json(self):
    return self.get_response().json()

  def _verify_file_exists(self, filePath):
    if not path.isfile(filePath):
      self.fail('File "%s" does not exist!. Failing test...' % filePath)
    self._archiveName = path.basename(filePath)
    self._doi = self.DOI_PREFFIX + self._archiveName[:-4]

  @ensure_api_called
  def verify_HTTP_code_is(self, httpCode):
    print 'Validating HTTP response code to be %s...' % httpCode,
    self.assertEqual(self._response.status_code, httpCode)
    print 'OK'

  @staticmethod
  def _run_tests_randomly():
    unittest.TestLoader.sortTestMethodsUsing = lambda _, x, y: random.choice([-1, 1])
    unittest.main()