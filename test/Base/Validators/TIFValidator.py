#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

'''
This class loads up an XML file in order to be used later on for validations against
API's responses.
'''

from Assert import Assert
from datetime import datetime


class TIFValidator(object):

  def __init__(self, name, data, xml):
    self._name = name
    self._size = len(data)
    self._xml = xml
    self.DOI_HEADER = 'info:doi/'
    self.DOI_PREFFIX = '10.1371/journal.'
    self.MIME = 'image/tiff'
    self.EXT = 'TIF'

  def get_size(self):
    return self._size

  def metadata(self, section, doi, testStartTime, apiTime):
    print 'Validating Graphics metadata section in response...',

    Assert.isNotNone(section)
    Assert.equals(section['doi'], doi)

    matchedXmlFile = self._xml.find(".//fig/label[contains(text(),'%s')]" % section['title'])
    Assert.isNotNone(matchedXmlFile)

    matchedXmlFile = self._xml.find(".//fig/caption/p[contains(text(),'%s')]" % section['description'])
    Assert.isNotNone(matchedXmlFile)

    xpath = ".//%s/*[@xlink:href='%s']" % \
     (section['contextElement'], section['original']['metadata']['doi'])
    matchedXmlFile = self._xml.find(xpath)

    Assert.isNotNone(matchedXmlFile)

    fileName = self.DOI_PREFFIX + self._name

    Assert.equals(section['original']['file'].lower(), fileName.lower())
    Assert.equals(section['original']['metadata']['doi'], doi)
    Assert.equals(section['original']['metadata']['contentType'], self.MIME)
    Assert.equals(section['original']['metadata']['extension'], self.EXT)
    Assert.equals(section['original']['metadata']['created'], section['original']['metadata']['lastModified'])
    Assert.equals(section['original']['metadata']['size'], self.get_size())
    self._verify_created_date(section['original']['metadata'], testStartTime, apiTime)
    print 'OK'


  def _verify_created_date(self, section, testStartTime, apiTime):
    # Some dates (PDF section) seem to include millis too, double check for possible bug?
    sectionDate = datetime.strptime(section['created'], '%Y-%m-%dT%H:%M:%S.%fZ')
    deltaTime = sectionDate - testStartTime
    Assert.isTrue(deltaTime.total_seconds() > 0)
    # Next validation is not working properly because there seems to be a difference of
    # around 7 hours between my box and one-leo.plosjournals.org environment (?)
    #Assert.isTrue(deltaTime.total_seconds() < apiTime)