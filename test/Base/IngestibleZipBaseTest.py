#!/usr/bin/env python2

'''
Base class for Rhino's Ingest related service tests.
'''

__author__ = 'jkrzemien@plos.org'

from JSONBasedServiceTest import JSONBasedServiceTest
from Config import RHINO_URL
from Validators.ZIPProcessor import ZIPProcessor
from Validators.Assert import Assert


class IngestibleZipBaseTest(JSONBasedServiceTest):

  INGESTION_API = RHINO_URL + '/zips'

  def zipUpload(self, archive, force_reingest=''):
    self._zip = ZIPProcessor(archive)

    daData = {"force_reingest": force_reingest}
    daFile = {'archive': open(archive, 'rb')}

    self.doPost(self.INGESTION_API, daData, daFile)

  def verify_doi_is_correct(self):
    print 'Validating DOI in response to be valid...',
    doiNodes = self._get_doi_from_response()
    for node in doiNodes:
      Assert.isTrue(len(node['doi']) > 0)
      Assert.isTrue(node['doi'].startswith(self._zip.get_full_doi()))
    print 'OK'

  def verify_article_xml_section(self):
    xml_section = self._get_article_xml_section()
    validator = self._zip.get_xml_validator()
    validator.metadata(xml_section, self._zip.get_doi(), self._testStartTime, self._apiTime)

  def verify_article_pdf_section(self):
    pdf_section = self._get_article_pdf_section()
    validator = self._zip.get_pdf_validator()
    validator.metadata(pdf_section, self._zip.get_doi(), self._testStartTime, self._apiTime)

  def verify_graphics_section(self):
    graphics_section = self._get_graphics_section()
    i = 1
    for section in graphics_section:
      fileInResponse = section['original']['file'].lower()
      validator = self._zip.get_graphics_validator(fileInResponse[16:])
      graphicDOI = self._zip.get_full_doi() + ('.e%03d' % i)
      validator.metadata(section, graphicDOI, self._testStartTime, self._apiTime)
      i += 1

  def verify_figures_section(self):
    figures_section = self._get_figures_section()
    i = 1
    for section in figures_section:
      fileInResponse = section['original']['file'].lower()
      validator = self._zip.get_figures_validator(fileInResponse[16:])
      graphicDOI = self._zip.get_full_doi() + ('.g%03d' % i)
      validator.metadata(section, graphicDOI, self._testStartTime, self._apiTime)
      i += 1