#!/usr/bin/env python2

"""
Base class for Rhino's Ingest related service tests.
"""

__author__ = 'jkrzemien@plos.org'

from ...Tests.BaseServiceTest import BaseServiceTest
from ...Config import API_BASE_URL
from ...Validators.ZIPProcessor import ZIPProcessor
from ...Decorators.Api import needs


ZIP_INGESTION_API = API_BASE_URL + '/zips'
INGESTIBLES_API = API_BASE_URL + '/ingestibles'


class IngestionCommon(BaseServiceTest):

  @needs('parsed', 'API')
  def verify_state_is(self, state):
    print 'Validating state in Response to be "%s"...' % state,
    stateNodes = self.parsed.get_state()
    for node in stateNodes:
      assert node['state'] == state, 'State field was expected to be "%s", but is: "%s"' % (state, node['state'])
    print 'OK'

  @needs('parsed', 'API')
  @needs('_zip', 'API')
  def verify_doi_is_correct(self):
    print 'Validating DOI in Response to be valid...',
    doiNodes = self.parsed.get_doi()
    for node in doiNodes:
      assert node['doi'] is not None, "DOI field in response was NULL!"
      assert len(node['doi']) > 0, "There should be some text in the DOI field, but was empty!"
      assert node['doi'].startswith(self._zip.get_full_doi()) == True, "DOI field did not start with %s" % self._zip.get_full_doi()
    print 'OK'

  @needs('parsed', 'API')
  @needs('_zip', 'API')
  def verify_article_xml_section(self):
    xml_section = self.parsed.get_article_xml_section()
    validator = self._zip.get_xml_validator()
    validator.metadata(xml_section, self._zip.get_doi(), self._testStartTime, self._apiTime)

  @needs('parsed', 'API')
  @needs('_zip', 'API')
  def verify_article_pdf_section(self):
    pdf_section = self.parsed.get_article_pdf_section()
    validator = self._zip.get_pdf_validator()
    validator.metadata(pdf_section, self._zip.get_doi(), self._testStartTime, self._apiTime)

  @needs('parsed', 'API')
  @needs('_zip', 'API')
  def verify_graphics_section(self):
    graphics_section = self.parsed.get_graphics_section()
    i = 1
    for section in graphics_section:
      fileInResponse = self._get_image_filename(section['original']['file'].lower())
      validator = self._zip.get_graphics_validator(fileInResponse)
      graphicDOI = self._zip.get_full_doi() + ('.e%03d' % i)
      validator.metadata(section, graphicDOI, self._testStartTime, self._apiTime)
      i += 1

  @needs('parsed', 'API')
  @needs('_zip', 'API')
  def verify_figures_section(self):
    figures_section = self.parsed.get_figures_section()
    i = 1
    for section in figures_section:
      fileInResponse = self._get_image_filename(section['original']['file'].lower())
      validator = self._zip.get_figures_validator(fileInResponse)
      graphicDOI = self._zip.get_full_doi() + ('.g%03d' % i)
      validator.metadata(section, graphicDOI, self._testStartTime, self._apiTime)
      i += 1


class ZIPIngestion(IngestionCommon):

  def zipUpload(self, archive, force_reingest=''):
    file = self.find_file(archive)
    self._zip = ZIPProcessor(file)

    daData = {"force_reingest": force_reingest}
    daFile = {'archive': open(file, 'rb')}

    self.doPost(ZIP_INGESTION_API, daData, daFile)

class Ingestibles(IngestionCommon):

  def ingest_archive(self, filename, force_reingest=''):

    daData = {'name': filename, 'force_reingest': force_reingest}

    self.doPost(INGESTIBLES_API, daData)


  def list_ingestibles(self):

    self.doGet(INGESTIBLES_API)


