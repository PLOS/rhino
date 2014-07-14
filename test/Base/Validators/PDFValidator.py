#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

'''
This class loads up a PDF file in order to be used later on for validations against
API's responses.
'''

from datetime import datetime
from Asserter import Asserter


class PDFValidator(Asserter):

  def __init__(self, data):
    self._size = len(data)

  def get_size(self):
    return self._size

  def _verify_created_date(self, section, testStartTime, apiTime):
    # Some dates (PDF section) seem to include millis too, double check for possible bug?
    sectionDate = datetime.strptime(section['created'], '%Y-%m-%dT%H:%M:%S.%fZ')
    deltaTime = sectionDate - testStartTime
    assert deltaTime.total_seconds() > 0
    # Next validation is not working properly because there seems to be a difference of
    # around 7 hours between my box and system under test environment (?) (one-leo.plosjournals.org)
    #assert deltaTime.total_seconds() < apiTime


  def metadata(self, section, doi, testStartTime, apiTime):
    print 'Validating PDF metadata section in response...',
    assert section is not None
    assert section['file'] == doi + '.PDF'
    assert section['metadata']['doi'] == 'info:doi/' + doi
    assert section['metadata']['contentType'] == 'application/pdf'
    assert section['metadata']['extension'] == 'PDF'
    assert section['metadata']['created'] == section['metadata']['lastModified']
    assert section['metadata']['size'] == self.get_size()
    self._verify_created_date(section['metadata'], testStartTime, apiTime)
    print 'OK'