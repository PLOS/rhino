#!/usr/bin/env python2

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

__author__ = 'jkrzemien@plos.org'

import unittest
import random
from WebDriverFactory import WebDriverFactory


class FrontEndTest(unittest.TestCase):

  """

  Base class to provide Front End tests with desired WebDriver instances, as defined in [[Config.py]].

  It inherits from `TestCase` in order to count as a test suite for Python's `unittest` framework.

  """

  # This defines any `FrontEndTest` derived class as able to be run by Nose in a parallel way.
  # Requires Nose's `MultiProcess` plugin to be *enabled*
  _multiprocess_can_split_ = True

  # Will contain a single driver instance for the current test
  _driver = None

  # Will contain a list of driver (not instantiated) for the current test variations (for all browsers)
  _injected_drivers = []

  # Factory object to instantiate drivers
  factory = WebDriverFactory()

  def setUp(self):
    pass

  def tearDown(self):
    """
    Method in charge of destroying the WebDriver/Proxy instances
    once the test finished running (even upon test failure).
    """
    if self._driver:
      self._driver.quit()
    else:
      self.factory.teardown_webdriver()

  def getDriver(self):
    """
    Simple method to retrieve the WebDriver/Proxy instances for this class to test method.
    """
    if not self._driver:
      if len(self._injected_drivers) > 0:
        self._driver = self.factory.setup_remote_webdriver(self._injected_drivers.pop())
      else:
        self._driver = self.factory.setup_webdriver()
    return self._driver

  @staticmethod
  def _run_tests_randomly():
    """
    *Static* method for every test suite inheriting this class to be able to run its tests
    in, at least, a non linear fashion.
    """
    unittest.TestLoader.sortTestMethodsUsing = lambda _, x, y: random.choice([-1, 1])
    unittest.main()
