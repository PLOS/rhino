#!/usr/bin/env python2

"""
Base class for Rhino's JSON based service tests.

Python's JSONPath can be installed via the following command:

  sudo pip install --allow-external jsonpath --allow-unverified jsonpath jsonpath

"""

__author__ = 'jkrzemien@plos.org'

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
    return self._json

  def jpath(self, path):
    return jsonpath(self._json, path)

  def get_doi(self):
    return self.jpath('$.[?(@.doi)]')

  def get_article_xml_section(self):
    return self.jpath('$..articleXml')[0]

  def get_article_pdf_section(self):
    return self.jpath('$..articlePdf')[0]

  def get_graphics_section(self):
    return self.jpath('$..graphics')[0]

  def get_figures_section(self):
    return self.jpath('$..figures')[0]

  def get_syndications_section(self):
    return self.jpath('$..syndications')[0]

  def get_state(self):
    return self.jpath('$.[?(@.state)]')