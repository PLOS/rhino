#!/usr/bin/env python2

'''
Base class for Rhino's Ingest related service tests.
'''

__author__ = 'jkrzemien@plos.org'

import requests
from datetime import datetime
from jsonpath import jsonpath
from Base.BaseServiceTest import BaseServiceTest
from Base.Config import RHINO_URL
from Base.Decorators import ensure_api_called, timeit
from zipfile import ZipFile


class IngestibleZipBaseTest(BaseServiceTest):

  def __init__(self, module):
    super(IngestibleZipBaseTest, self).__init__(module)

    self.API_UNDER_TEST = RHINO_URL + '/zips'

  @timeit
  def zipUpload(self, archive, force_reingest=''):
    self._verify_file_exists(archive)
    self._zip = ZipFile(archive, "r")

    daData = {"force_reingest": force_reingest}
    daFile = {'archive': open(archive, 'rb')}

    self._testStartTime = datetime.now()
    self._response = requests.post(self.API_UNDER_TEST, data=daData, files=daFile, verify=False)
    self._apiTime = (datetime.now() - self._testStartTime).total_seconds()

    self._jsonResponse = self.get_response_as_json()
    self._textResponse = self.get_response_as_text()

  @ensure_api_called
  def verify_doi_is_correct(self):
    print 'Validating DOI in response to be "%s" related...' % self._doi,
    doiNodes = jsonpath(self._jsonResponse, "$.[?(@.doi)]")
    for node in doiNodes:
      self.assertTrue(node['doi'].startswith(self._doi))
    print 'OK'

  @ensure_api_called
  def verify_state_is(self, state):
    print 'Validating state in response to be "%s"...' % state,
    stateNodes = jsonpath(self._jsonResponse, "$.[?(@.state)]")
    for node in stateNodes:
      self.assertEqual(node['state'], state)
    print 'OK'

  def _verify_created_date(self, node):
    # Some dates (PDF section) seem to include millis too, double check for possible bug?
    try:
      nodeDate = datetime.strptime(node['created'], '%Y-%m-%dT%H:%M:%SZ')
    except ValueError:
      nodeDate = datetime.strptime(node['created'], '%Y-%m-%dT%H:%M:%S.%fZ')

    deltaTime = nodeDate - self._testStartTime
    self.assertTrue(deltaTime.total_seconds() > 0)
    # Next validation is not working properly because there seems to be a difference of
    # around 7 hours between my box and one-leo.plosjournals.org environment (?)
    #assert deltaTime.total_seconds() < self._apiTime

  def _verify_section(self, jsonPath, zipInfo, extension, mimeType):
    print 'Validating %s section in response...' % extension,
    node = jsonpath(self._jsonResponse, jsonPath)[0]
    assert node is not None
    self.assertEqual(node['file'], self._doi[9:] + '.' + extension.upper())
    self.assertEqual(node['metadata']['doi'], self._doi) # Already checked
    self.assertEqual(node['metadata']['contentType'], mimeType)
    self.assertEqual(node['metadata']['extension'], extension.upper())
    self.assertEqual(node['metadata']['created'], node['metadata']['lastModified'])
    self.assertEqual(node['metadata']['size'], zipInfo.file_size)
    self._verify_created_date(node['metadata'])
    print 'OK'

  @ensure_api_called
  def verify_article_xml_section(self):
    xmlInfo = self._zip.getinfo(self._archiveName[:-3] + 'xml')
    self._verify_section('$..articleXml', xmlInfo, 'XML', 'text/xml')

  @ensure_api_called
  def verify_article_pdf_section(self):
    pdfInfo = self._zip.getinfo(self._archiveName[:-3] + 'pdf')
    self._verify_section('$..articlePdf', pdfInfo, 'PDF', 'application/pdf')

  @ensure_api_called
  def verify_graphics_section(self):
    print 'Validating Graphics section in response...',
    graphicsNodes = jsonpath(self._jsonResponse, "$..graphics")[0]
    assert graphicsNodes is not None
    i = 0
    for node in graphicsNodes:
      i += 1
      assert node is not None
      suffix = '.e%03d' % i
      self.assertEqual(node['doi'], (self._doi + suffix))
      #graphInfo = self._zip.getinfo(self._archiveName[:-3] + suffix + '.TIF') # Are these always TIF?
    print 'OK'
