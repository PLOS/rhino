#!/usr/bin/env python2

"""
Base class for Rhino related service tests.
"""

__author__ = 'jkrzemien@plos.org'

import unittest
import random
import json
from os import walk
from os.path import dirname, abspath
from inspect import getfile

from requests import get, post, patch, put, delete

from ..Validators.Assert import Assert
from ..Decorators.Injection import store_as_entity
from ..Decorators.Api import timeit
from ..Config import TIMEOUT, PRINT_DEBUG


class BaseServiceTest(unittest.TestCase):
  _http = None
  _testStartTime = None
  _apiTime = None

  # Autowired by [[store_as_entity#Injection.py]] decorator
  _parsed_response = None

  def setUp(self):
    pass

  def tearDown(self):
    self._http = None
    self._testStartTime = None
    self._apiTime = None

  def _debug(self):
    if PRINT_DEBUG:
      print 'API Response = %s' % self._http.text

  @store_as_entity
  @timeit
  def doGet(self, url, params=None):
    self._http = get(url, params=params, verify=False, timeout=TIMEOUT, allow_redirects=True)
    self._debug()

  @store_as_entity
  @timeit
  def doPost(self, url, data=None, files=None):
    self._http = post(url, data=data, files=files, verify=False, timeout=TIMEOUT, allow_redirects=True)
    self._debug()

  @store_as_entity
  @timeit
  def doPatch(self, url, data=None):
    self._http = patch(url, data=json.dumps(data), verify=False, timeout=TIMEOUT, allow_redirects=True)
    self._debug()

  @store_as_entity
  @timeit
  def doDelete(self, url, data=None):
    self._http = delete(url, data=data, verify=False, timeout=TIMEOUT, allow_redirects=True)
    self._debug()

  @store_as_entity
  @timeit
  def doPut(self, url, data=None):
    self._http = put(url, data=data, verify=False, timeout=TIMEOUT, allow_redirects=True)
    self._debug()

  @store_as_entity
  @timeit
  def doUpdate(self, url, data=None):
    self.doPut(url, data)

  def http(self):
    return self._http

  def verify_http_code_is(self, httpCode):
    print 'Validating HTTP Response code to be %s...' % httpCode,
    Assert.equals(self._http.status_code, httpCode)
    print 'OK'

  def get_parsed_response(self):
    return self._parsed_response

  def find_file(self, filename):
    path = dirname(abspath(getfile(BaseServiceTest))) + '/../../'
    for root, dirs, files in walk(path):
      for file in files:
        if file == filename:
          return root + '/' + file

  @staticmethod
  def _run_tests_randomly():
    unittest.TestLoader.sortTestMethodsUsing = lambda _, x, y: random.choice([-1, 1])
    unittest.main()
