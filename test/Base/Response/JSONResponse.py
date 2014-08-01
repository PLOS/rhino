#!/usr/bin/env python2

"""
Base class for Rhino's JSON based service tests.

Python's JSONPath can be installed via the following command:

  sudo pip install --allow-external jsonpath --allow-unverified jsonpath jsonpath

"""

__author__ = 'jkrzemien@plos.org'

import json
from jsonpath import jsonpath
from ..Validators.Assert import Assert
from AbstractResponse import AbstractResponse


class JSONResponse(AbstractResponse):

  _json = None

  def __init__(self, response):
    self._json = json.loads(response)

  def _jpath(self, path):
    return jsonpath(self._json, path)

  def get_doi_from_response(self):
    return self._jpath('$.[?(@.doi)]')

  def get_article_xml_section(self):
    return self._jpath('$..articleXml')[0]

  def get_article_pdf_section(self):
    return self._jpath('$..articlePdf')[0]

  def get_graphics_section(self):
    return self._jpath('$..graphics')[0]

  def get_figures_section(self):
    return self._jpath('$..figures')[0]

  def get_syndications_section(self):
    return self._jpath('$..syndications')[0]

  def get_state(self):
    return self._jpath('$.[?(@.state)]')