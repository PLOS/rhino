#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# Copyright (c) 2017 Public Library of Science
#
# Permission is hereby granted, free of charge, to any person obtaining a
# copy of this software and associated documentation files (the "Software"),
# to deal in the Software without restriction, including without limitation
# the rights to use, copy, modify, merge, publish, distribute, sublicense,
# and/or sell copies of the Software, and to permit persons to whom the
# Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
# THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
# DEALINGS IN THE SOFTWARE.

"""
Base class for Rhino related service tests.
"""

from inspect import getfile
import logging
from os import walk
from os.path import dirname, abspath
import random
from requests import get, post, patch, put, delete

import unittest

from teamcity import is_running_under_teamcity
from teamcity.unittestpy import TeamcityTestRunner

from test.api.Response.JSONResponse import JSONResponse
from .api import timeit
from .Config import TIMEOUT, PRINT_DEBUG

__author__ = 'jgray@plos.org'


class BaseServiceTest(unittest.TestCase):
    response = None

    # Autowired by @timeit decorator
    testStartTime = None

    # Autowired by @timeit decorator
    apiTime = None

    def setUp(self):
        pass

    def tearDown(self):
        self.response = None
        self.testStartTime = None
        self.apiTime = None

    @staticmethod
    def _debug(response):
        if PRINT_DEBUG:
            logging.info('API Response = {0}'.format(response.text))
            logging.info('API Response code = {0}'.format(response.status_code))

    @timeit
    def doGet(self, url, params=None, headers=None):
        response = get(url, headers=headers, params=params, verify=False, timeout=TIMEOUT,
                       allow_redirects=True)
        self._debug(response)
        return response

    @timeit
    def doPostData(self, url, data=None, headers=None):
        response = post(url, headers=headers, data=data, verify=False, timeout=TIMEOUT,
                        allow_redirects=True)
        self._debug(response)
        return response

    @timeit
    def doPost(self, url, files=None, data=None, headers=None):
        response = post(url, headers=headers, files=files, data=data, verify=False,
                        timeout=TIMEOUT, allow_redirects=True)
        self._debug(response)
        return response

    @timeit
    def doPatch(self, url, data=None, headers=None):
        response = patch(url, headers=headers, data=data, verify=False, timeout=TIMEOUT,
                         allow_redirects=True)
        self._debug(response)
        return response

    @timeit
    def doDelete(self, url, data=None, headers=None):
        response = delete(url, headers=headers, data=data, verify=False, timeout=TIMEOUT,
                          allow_redirects=True)
        self._debug(response)
        return response

    @timeit
    def doPut(self, url, data=None, headers=None):
        response = put(url, headers=headers, data=data, verify=False, timeout=TIMEOUT,
                       allow_redirects=True)
        self._debug(response)
        return response

    @timeit
    def doUpdate(self, url, data=None, headers=None):
        self.doPut(url, data, headers)

    def get_http_response(self):
        return self.response

    def parse_response_as_json(self, response):
        self.parsed = JSONResponse(response.text)

    def verify_http_code_is(self, response, http_code):
        logging.info('Validating HTTP Response code to be {0}...'.format(http_code))
        assert response.status_code == http_code
        logging.info('OK')

    def find_file(self, filename):
        path = dirname(abspath(getfile(BaseServiceTest))) + '/../../'
        for root, dirs, files in walk(path):
            for file in files:
                if file == filename:
                    return root + '/' + file

    @staticmethod
    def run_tests_randomly():
        unittest.TestLoader.sortTestMethodsUsing = lambda _, x, y: random.choice([-1, 1])
        if is_running_under_teamcity():
            runner = TeamcityTestRunner()
        else:
            runner = unittest.TextTestRunner()
        unittest.main(testRunner=runner)
