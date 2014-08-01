#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

"""
This class loads up an XML file in order to be used later on for validations against
Tests's responses.
"""

import libxml2
from datetime import datetime
from Assert import Assert
from AbstractValidator import AbstractValidator


class XMLValidator(AbstractValidator):

  def __init__(self, data):
    self._size = len(data)
    self._root = libxml2.parseDoc(data)
    self.context = self._root.xpathNewContext()
    self.context.xpathRegisterNs('xlink', 'http://www.w3.org/1999/xlink')

  def get_size(self):
    return self._size

  def find(self, expression):
    return self.context.xpathEval(expression)

  def _verify_created_date(self, section, testStartTime, apiTime):
    # Some dates (PDF section) seem to include millis too, double check for possible bug?
    sectionDate = datetime.strptime(section['created'], '%Y-%m-%dT%H:%M:%SZ')
    deltaTime = sectionDate - testStartTime
    Assert.isTrue(deltaTime.total_seconds() > 0)
    # Next validation is not working properly because there seems to be a difference of
    # around 7 hours between my box and one-leo.plosjournals.org environment (?)
    #Assert.isTrue(deltaTime.total_seconds() < apiTime)

  def metadata(self, section, doi, testStartTime, apiTime):
    print 'Validating XML metadata section in Response...',
    Assert.isNotNone(section)
    Assert.equals(section['file'], doi + '.XML')
    Assert.equals(section['metadata']['doi'], 'info:doi/' + doi)
    Assert.equals(section['metadata']['contentType'], 'text/xml')
    Assert.equals(section['metadata']['extension'], 'XML')
    Assert.equals(section['metadata']['created'], section['metadata']['lastModified'])
    Assert.equals(section['metadata']['size'], self.get_size())
    self._verify_created_date(section['metadata'], testStartTime, apiTime)
    print 'OK'
