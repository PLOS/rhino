#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# Copyright (c) 2018 Public Library of Science
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
Base class for Configuration crud controller
"""

from ...Base.base_service_test import BaseServiceTest
from ..resources import API_BASE_URL, OK, BAD_REQUEST

__author__ = 'jgray@plos.org'

CONFIGURATION_API = API_BASE_URL + '/config'


class Configuration(BaseServiceTest):
    def setUp(self):
        self.already_done = 0

    def tearDown(self):
        """
        Purge all objects and collections created in the test case
        """
        if self.already_done > 0:
            return

    def get_type(self, type_):
        """
        Calls configuration-read-controller API with type parameter
        :param type_: string. Valid values are 'build', 'run' or 'repo'
        """
        response = self.doGet(CONFIGURATION_API, params='type={0}'.format(type_))
        if type_ in ('build', 'run', 'repo'):
            self.verify_http_code_is(response, OK)
            self.parse_response_as_json(response)
        else:
            self.verify_http_code_is(response, BAD_REQUEST)
