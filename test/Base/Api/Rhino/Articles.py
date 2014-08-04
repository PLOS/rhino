#!/usr/bin/env python2

"""
Base class for Rhino's Articles related service tests.
"""

__author__ = 'jkrzemien@plos.org'

from ...Tests.BaseServiceTest import BaseServiceTest
from ...Validators.ZIPProcessor import ZIPProcessor
from ...Decorators.Api import deduce_doi, needs
from ...Config import API_BASE_URL

ARTICLES_API = API_BASE_URL + '/articles/'

class Articles(BaseServiceTest):

  def define_zip_file_for_validations(self, archive):
    file = self.find_file(archive)
    self._zip = ZIPProcessor(file)

  @deduce_doi
  def updateArticle(self, article, state, syndications=None):

    data = {'state': state }

    if syndications is not None:
      data['syndications'] = syndications

    self.doPatch(ARTICLES_API + article, data)

    self.parse_response_as_json()

  def verify_state_is(self, state):
    print 'Validating state in Response to be "%s"...' % state,
    stateNodes = self.parsed.get_state()
    for node in stateNodes:
      assert node['state'] == state, "State field was expected to be %s, but is: %s" % (state, node['state'])
    print 'OK'


  def verify_doi_is_correct(self):
    print 'Validating DOI in Response are present...',
    doiNodes = self.parsed.get_doi()
    for node in doiNodes:
      assert node['doi'] is not None, "DOI field in response was NULL!"
      assert len(node['doi']) > 0, "There should be some text in the DOI field, but was empty!"
    print 'OK'

  @needs('_zip', 'define_zip_file_for_validations')
  def verify_article_xml_section(self):
    xml_section = self.parsed.get_article_xml_section()
    validator = self._zip.get_xml_validator()
    validator.metadata(xml_section, self._zip.get_doi(), self._testStartTime, self._apiTime)

  @needs('_zip', 'define_zip_file_for_validations')
  def verify_article_pdf_section(self):
    pdf_section = self.parsed.get_article_pdf_section
    validator = self._zip.get_pdf_validator()
    validator.metadata(pdf_section, self._zip.get_doi(), self._testStartTime, self._apiTime)

  @needs('_zip', 'define_zip_file_for_validations')
  def verify_graphics_section(self):
    graphics_section = self.parsed.get_graphics_section()
    i = 1
    for section in graphics_section:
      fileInResponse = self._get_image_filename(section['original']['file'].lower())
      validator = self._zip.get_graphics_validator(fileInResponse)
      graphicDOI = self._zip.get_full_doi() + ('.e%03d' % i)
      validator.metadata(section, graphicDOI, self._testStartTime, self._apiTime)
      i += 1

  @needs('_zip', 'define_zip_file_for_validations')
  def verify_figures_section(self):
    figures_section = self.parsed.get_figures_section()
    i = 1
    for section in figures_section:
      fileInResponse = self._get_image_filename(section['original']['file'].lower())
      validator = self._zip.get_figures_validator(fileInResponse)
      graphicDOI = self._zip.get_full_doi() + ('.g%03d' % i)
      validator.metadata(section, graphicDOI, self._testStartTime, self._apiTime)
      i += 1

  def verify_syndications_status_is(self, expected_syndications_state):
    syndications_section = self.parsed.get_syndications_section()
    self.assertDictContainsSubset(expected_syndications_state, syndications_section)