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

"""
Base class for Rhino's JSON based service tests.
Python's JSONPath can be installed via the following command:
  sudo pip install --allow-external jsonpath --allow-unverified jsonpath jsonpath
"""

__author__ = 'jgray@plos.org'

import json

from jsonpath import jsonpath

from api.Response.AbstractResponse import AbstractResponse


class JSONResponse(AbstractResponse):

  _json = None

  def __init__(self, response):
    try:
      self._json = json.loads(response)
    except Exception as e:
      print('Error while trying to parse response as JSON!')
      print('Actual response was: "%s"' % response)
      raise e

  def get_json(self, printvalue=True):
    if printvalue:
      print(self._json)
    return self._json

  def jpath(self, path):
    return jsonpath(self._json, path)

  def get_journals(self):
    return self.jpath('$[]')

  def get_journalKey(self):
    return self.jpath('$..journalKey')

  def get_journaleIssn(self):
    return self.jpath('$..eIssn')

  def get_journalTitle(self):
    return self.jpath('$..title')

  def get_attribute(self, name):
      return self._json.get(name, None)

  def get_list(self):
    return self.jpath('$[*]')

  def get_article_doi(self):
    return self.jpath('$..doi')

  def get_article_revision_number(self):
    return self.jpath('$..revisionNumber')
