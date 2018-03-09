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

__author__ = 'jgray@plos.org'

"""
This test cases validates JSON configuration crud controller.
"""

import logging

from ..Base.base_service_test import BaseServiceTest
from test.api.RequestObject.configuration import Configuration


class ConfigurationTest(Configuration):

    def test_smoke_configuration(self):
        logging.info('Configuration test smoke')
        self.get_type(type_='run')
        self.get_type(type_='build')
        self.get_type(type_='repo')

    def test_core_configuration(self):
        logging.info('Configuration test core')
        self.get_type(type_='bogus')


if __name__ == '__main__':
    BaseServiceTest.run_tests_randomly()
