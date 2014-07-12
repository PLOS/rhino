#!/usr/bin/env python2

'''
Base class for Rhino's JSON based service tests.
'''

__author__ = 'jkrzemien@plos.org'

from jsonpath import jsonpath
from BaseServiceTest import BaseServiceTest
from Decorators.Api import ensure_api_called


class JSONBasedServiceTest(BaseServiceTest):

  def __init__(self, module):
    super(JSONBasedServiceTest, self).__init__(module)

  @ensure_api_called
  def get_response_as_json(self):
    return self.get_response().json()

  def _jpath(self, path):
    return jsonpath(self.get_response_as_json(), path)

  def _get_doi_from_response(self):
    return self._jpath('$.[?(@.doi)]')

  def _get_article_xml_section(self):
    return self._jpath('$..articleXml')[0]

  def _get_article_pdf_section(self):
    return self._jpath('$..articlePdf')[0]

  def _get_graphics_section(self):
    return self._jpath('$..graphics')[0]

  def _get_figures_section(self):
    return self._jpath('$..figures')[0]

  def verify_state_is(self, state):
    print 'Validating state in response to be "%s"...' % state,
    stateNodes = self._jpath('$.[?(@.state)]')
    for node in stateNodes:
      self.assertEqual(node['state'], state)
    print 'OK'
