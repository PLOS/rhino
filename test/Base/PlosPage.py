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

import logging

from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as expected_conditions
from selenium.webdriver.common.action_chains import ActionChains
from selenium.common.exceptions import TimeoutException
from .CustomException import ElementDoesNotExistAssertionError
from bs4 import BeautifulSoup
from .LinkVerifier import LinkVerifier
from .CustomExpectedConditions import element_to_be_clickable
from .Config import wait_timeout, environment, base_url

__author__ = 'jkrzemien@plos.org'


class PlosPage(object):
    """
    Model an abstract base Journal page.
    """
    PROD_URL = ''

    def __init__(self, driver, url_suffix=''):

        # Internal WebDriver-related protected members
        self._driver = driver
        self._wait = WebDriverWait(self._driver, wait_timeout)
        self._actions = ActionChains(self._driver)

        base_url_ = self.__build_environment_url(url_suffix)

        # Prevents WebDriver from navigating to a page more than once (there should be only one
        # starting point for a test)
        if not hasattr(self._driver, 'navigated'):
            try:
                self._driver.get(base_url_)
                self._driver.navigated = True
            except TimeoutException as toe:
                logging.error(
                        '\t[WebDriver Error] WebDriver timed out while trying to load the '
                        'requested web page "{0}".'.format(base_url_))
                raise toe

        # Internal private member
        self.__linkVerifier = LinkVerifier()

        # Locators - Instance variables unique to each instance
        self._article_type_menu = (By.ID, 'article-type-menu')

    # POM Actions

    def __build_environment_url(self, url_suffix):
        """
        *Private* method to detect on which environment we are running the test.
        Then builds up a URL accordingly

        1. url_suffix: String representing the suffix to append to the URL.

        **Returns** A string representing the whole URL from where our test starts

        """
        env = environment.lower()
        base_url_ = self.PROD_URL if env == 'prod' else base_url + url_suffix
        return base_url_

    def _get(self, locator):
        try:
            return self._wait.until(expected_conditions.visibility_of_element_located(locator))
        except TimeoutException:
            logging.error(
                    '\t[WebDriver Error] WebDriver timed out while trying to identify element '
                    'by {0}.'.format(locator))
            raise ElementDoesNotExistAssertionError(locator)

    def _gets(self, locator):
        try:
            return self._wait.until(expected_conditions.presence_of_all_elements_located(locator))
        except TimeoutException:
            logging.error(
                    '\t[WebDriver Error] WebDriver timed out while trying to identify element '
                    'by {0!s}.'.format(locator))
            raise ElementDoesNotExistAssertionError(locator)

    def _wait_for_element(self, element, multiplier=1):
        """
        We need a method that can be used to determine whether a page comprised of dynamic elements
          has fully loaded, or loaded enough to expose element.
        :param element: the item on a dynamic page we want to wait for
        :param multiplier: a multiplier, default (5) applied against the base wait_timeout to wait
          for element
        """
        timeout = wait_timeout * multiplier
        self.set_timeout(timeout)
        self._wait.until(element_to_be_clickable(element))
        self.restore_timeout()

    def _is_link_valid(self, link):
        return self.__linkVerifier.is_link_valid(link.get_attribute('href'))

    def traverse_to_frame(self, frame):
        logging.info('\t[WebDriver] About to switch to frame "{0}"...'.format(frame))
        self._wait.until(expected_conditions.frame_to_be_available_and_switch_to_it(frame))
        logging.info('OK')

    def traverse_from_frame(self):
        logging.info('\t[WebDriver] About to switch to default content...')
        self._driver.switch_to.default_content()
        logging.info('OK')

    def set_timeout(self, new_timeout):
        self._driver.implicitly_wait(new_timeout)
        self._wait = WebDriverWait(self._driver, new_timeout)

    def restore_timeout(self):
        self._driver.implicitly_wait(wait_timeout)
        self._wait = WebDriverWait(self._driver, wait_timeout)

    def get_text(self, s):
        soup = BeautifulSoup(s)
        clean_out = soup.get_text()
        logging.info(clean_out)
        return clean_out

    def refresh(self):
        """
        refreshes the whole page
        """
        self._driver.refresh()
