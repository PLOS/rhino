#!/usr/bin/env python2

"""
Base class for Rhino's Articles related service tests.
"""

__author__ = 'jkrzemien@plos.org'

from ..Tests.BaseServiceTest import BaseServiceTest
from ..Validators.ZIPProcessor import ZIPProcessor
from ..Decorators.Api import deduce_doi, ensure_zip_provided
from ..Validators.Assert import Assert
from ..Config import RHINO_URL


class Articles(BaseServiceTest):

  ARTICLES_API = RHINO_URL + '/articles/'

  def define_zip_file_for_validations(self, archive):
    file = self.find_file(archive)
    self._zip = ZIPProcessor(file)

  @deduce_doi
  def updateArticle(self, article, state, syndications=None):
    Assert.isNotNone(article)
    Assert.isNotNone(state)

    data = {'state': state }

    if syndications is not None:
      data['syndications'] = syndications

    self.doPatch(self.ARTICLES_API + article, data)

  def verify_state_is(self, state):
    print 'Validating state in Response to be "%s"...' % state,
    stateNodes = self._parsed_response.get_state()
    for node in stateNodes:
      Assert.equals(node['state'], state)
    print 'OK'


  def verify_doi_is_correct(self):
    print 'Validating DOI in Response are present...',
    doiNodes = self._parsed_response.get_doi_from_response()
    for node in doiNodes:
      Assert.isNotNone(node['doi'])
      Assert.isTrue(len(node['doi']) > 0)
    print 'OK'

  @ensure_zip_provided
  def verify_article_xml_section(self):
    xml_section = self._parsed_response.get_article_xml_section()
    validator = self._zip.get_xml_validator()
    validator.metadata(xml_section, self._zip.get_doi(), self._testStartTime, self._apiTime)

  @ensure_zip_provided
  def verify_article_pdf_section(self):
    pdf_section = self._parsed_response.get_article_pdf_section()
    validator = self._zip.get_pdf_validator()
    validator.metadata(pdf_section, self._zip.get_doi(), self._testStartTime, self._apiTime)

  @ensure_zip_provided
  def verify_graphics_section(self):
    graphics_section = self._parsed_response.get_graphics_section()
    i = 1
    for section in graphics_section:
      fileInResponse = section['original']['file'].lower()
      validator = self._zip.get_graphics_validator(fileInResponse[16:])
      graphicDOI = self._zip.get_full_doi() + ('.e%03d' % i)
      validator.metadata(section, graphicDOI, self._testStartTime, self._apiTime)
      i += 1

  @ensure_zip_provided
  def verify_figures_section(self):
    figures_section = self._parsed_response.get_figures_section()
    i = 1
    for section in figures_section:
      fileInResponse = section['original']['file'].lower()
      validator = self._zip.get_figures_validator(fileInResponse[16:])
      graphicDOI = self._zip.get_full_doi() + ('.g%03d' % i)
      validator.metadata(section, graphicDOI, self._testStartTime, self._apiTime)
      i += 1

  def verify_syndications_status_is(self, expected_syndications_state):
    syndications_section = self._parsed_response.get_syndications_section()
    self.assertDictContainsSubset(expected_syndications_state, syndications_section)