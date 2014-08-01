#!/usr/bin/env python2

"""
  Base class for Rhino's XML based service tests.
"""

__author__ = 'jkrzemien@plos.org'

import libxml2

from ..Validators.Assert import Assert
from AbstractResponse import AbstractResponse


class XMLResponse(AbstractResponse):

  _xml = None

  def __init__(self, response):
    self._xml = libxml2.parseDoc(response)

  def _xpath(self, path):
    return self._xml.xpathEval(path)

  def get_doi_from_response(self):
    return self._xpath('$.[?(@.doi)]')

  def get_article_xml_section(self):
    return self._xpath('$..articleXml')[0]

  def get_article_pdf_section(self):
    return self._xpath('$..articlePdf')[0]

  def get_graphics_section(self):
    return self._xpath('$..graphics')[0]

  def get_figures_section(self):
    return self._xpath('$..figures')[0]

  def get_syndications_section(self):
    return self._xpath('$..syndications')[0]

  def get_state(self):
    return self._xpath('$.[?(@.state)]')