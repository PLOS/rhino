#!/usr/bin/env python2

"""
  Base class for Rhino's XML based service tests.
  Currently, there is *no* API that returns an XML as the API *response*.
  There are APIs that return an actual XML file that was previously ingested.
  Example:
  http://one-fluffy.plosjournals.org/api/assetfiles/10.1371/journal.pone.0097823.xml
"""

__author__ = 'jkrzemien@plos.org'

import libxml2

from AbstractResponse import AbstractResponse


class XMLResponse(AbstractResponse):

  _xml = None

  def __init__(self, response):
    try:
      self._xml = libxml2.parseDoc(response.encode("UTF-8"))
    except Exception as e:
      print 'Error while trying to parse response as XML!'
      print 'Actual response was: "%s"' % response
      raise e

  def get_xml(self):
    return self._xml

  def xpath(self, path):
    return self._xml.xpathEval(path)

  def get_doi(self):
    return self.xpath("//article-id[@pub-id-type='doi']")

  def get_article_xml_section(self):
    return self.xpath('//something')

  def get_article_pdf_section(self):
    return self.xpath('//something')

  def get_graphics_section(self):
    return self.xpath('//something')

  def get_figures_section(self):
    return self.xpath('//something')

  def get_syndications_section(self):
    return self.xpath('//something')

  def get_state(self):
    return self.xpath('//something')