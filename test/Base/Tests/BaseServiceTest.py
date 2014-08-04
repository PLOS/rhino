#!/usr/bin/env python2

"""
Base class for Rhino related service tests.
"""

__author__ = 'jkrzemien@plos.org'

import unittest
import random
import json
import re
from os import walk
from os.path import dirname, abspath
from inspect import getfile

from requests import get, post, patch, put, delete

from ..Decorators.Api import timeit

from ..Config import TIMEOUT, PRINT_DEBUG
from ..Response.JSONResponse import JSONResponse
from ..Response.XMLResponse import XMLResponse


IMAGE_FILE_PATTERN = re.compile('\w+\.\d+\.(e|g)\d{3}.(png|tif)$')


class BaseServiceTest(unittest.TestCase):

  __response = None

  # Autowired by @timeit decorator
  _testStartTime = None

  # Autowired by @timeit decorator
  _apiTime = None

  def setUp(self):
    pass

  def tearDown(self):
    self.__response = None
    self._testStartTime = None
    self._apiTime = None

  def _debug(self):
    if PRINT_DEBUG:
      print 'API Response = %s' % self.__response.text

  @timeit
  def doGet(self, url, params=None):
    self.__response = get(url, params=params, verify=False, timeout=TIMEOUT, allow_redirects=True)
    self._debug()

  @timeit
  def doPost(self, url, data=None, files=None):
    self.__response = post(url, data=data, files=files, verify=False, timeout=TIMEOUT, allow_redirects=True)
    self._debug()

  @timeit
  def doPatch(self, url, data=None):
    self.__response = patch(url, data=json.dumps(data), verify=False, timeout=TIMEOUT, allow_redirects=True)
    self._debug()

  @timeit
  def doDelete(self, url, data=None):
    self.__response = delete(url, data=data, verify=False, timeout=TIMEOUT, allow_redirects=True)
    self._debug()

  @timeit
  def doPut(self, url, data=None):
    self.__response = put(url, data=data, verify=False, timeout=TIMEOUT, allow_redirects=True)
    self._debug()

  @timeit
  def doUpdate(self, url, data=None):
    self.doPut(url, data)

  def get_http_response(self):
    return self.__response

  def parse_response_as_xml(self):
    self.parsed = XMLResponse(self.get_http_response().text)

  def parse_response_as_json(self):
    self.parsed = JSONResponse(self.get_http_response().text)

  def verify_http_code_is(self, httpCode):
    print 'Validating HTTP Response code to be %s...' % httpCode,
    self.assertEquals(self.__response.status_code, httpCode)
    print 'OK'

  def find_file(self, filename):
    path = dirname(abspath(getfile(BaseServiceTest))) + '/../../'
    for root, dirs, files in walk(path):
      for file in files:
        if file == filename:
          return root + '/' + file

  def _get_image_filename(self, filename):
    return re.search(IMAGE_FILE_PATTERN, filename).group(0)

  @staticmethod
  def _run_tests_randomly():
    unittest.TestLoader.sortTestMethodsUsing = lambda _, x, y: random.choice([-1, 1])
    unittest.main()
