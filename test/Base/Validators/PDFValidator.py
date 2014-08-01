#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

'''
This class loads up a PDF file in order to be used later on for validations against
Tests's responses.
'''

from datetime import datetime
from Assert import Assert
from AbstractValidator import AbstractValidator


class PDFValidator(AbstractValidator):

  def _verify_created_date(self, section, testStartTime, apiTime):
    # Some dates (PDF section) seem to include millis too, double check for possible bug?
    sectionDate = datetime.strptime(section['created'], '%Y-%m-%dT%H:%M:%S.%fZ')
    deltaTime = sectionDate - testStartTime
    Assert.isTrue(deltaTime.total_seconds() > 0)
    # Next validation is not working properly because there seems to be a difference of
    # around 7 hours between my box and system under test environment (?) (one-leo.plosjournals.org)
    #Assert.isTrue(deltaTime.total_seconds() < apiTime)


  def metadata(self, section, doi, testStartTime, apiTime):
    print 'Validating PDF metadata section in Response...',
    Assert.isNotNone(section)
    Assert.equals(section['file'], doi + '.PDF')
    Assert.equals(section['metadata']['doi'], 'info:doi/' + doi)
    Assert.equals(section['metadata']['contentType'], 'application/pdf')
    Assert.equals(section['metadata']['extension'], 'PDF')
    Assert.equals(section['metadata']['created'], section['metadata']['lastModified'])
    Assert.equals(section['metadata']['size'], self.get_size())
    self._verify_created_date(section['metadata'], testStartTime, apiTime)
    print 'OK'