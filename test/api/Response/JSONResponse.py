#!/usr/bin/env python2

"""
Base class for Rhino's JSON based service tests.
Python's JSONPath can be installed via the following command:
  sudo pip install --allow-external jsonpath --allow-unverified jsonpath jsonpath
"""

__author__ = 'jgray@plos.org'

import json
from jsonpath import jsonpath
from AbstractResponse import AbstractResponse


class JSONResponse(AbstractResponse):

  _json = None

  def __init__(self, response):
    try:
      self._json = json.loads(response)
    except Exception as e:
      print 'Error while trying to parse response as JSON!'
      print 'Actual response was: "%s"' % response
      raise e

  def get_json(self):
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


