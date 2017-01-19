#!/usr/bin/env python2

# Copyright (c) 2017 Public Library of Science
#
# Permission is hereby granted, free of charge, to any person obtaining a
# copy of this software and associated documentation files (the "Software"),
# to deal in the Software without restriction, including without limitation
# the rights to use, copy, modify, merge, publish, distribute, sublicense,
# and/or sell copies of the Software, and to permit persons to whom the
# Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
# THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
# DEALINGS IN THE SOFTWARE.

__author__ = 'jkrzemien@plos.org'

'''
This class loads up a PDF file in order to be used later on for validations against
Tests's responses.
'''

from datetime import datetime

from AbstractValidator import AbstractValidator


class PDFValidator(AbstractValidator):

  def _verify_created_date(self, section, testStartTime, apiTime):
    # Some dates (PDF section) seem to include millis too, double check for possible bug?
    sectionDate = datetime.strptime(section['created'], '%Y-%m-%dT%H:%M:%S.%fZ')
    deltaTime = sectionDate - testStartTime
    assert deltaTime.total_seconds() > 0, "Created field in metadata section should be greater than test start time!"
    # Next validation is not working properly because there seems to be a difference of
    # around 7 hours between my box and system under test environment (?) (one-leo.plosjournals.org)
    #assert apiTime > deltaTime.total_seconds(), "API invocation time should be greater than diff between Created field in metadata section & test start time!"


  def metadata(self, section, doi, testStartTime, apiTime):
    print 'Validating PDF metadata section in Response...',
    assert section is not None
    assert section['file'] == doi + '.PDF', "File field in metadata section did not match!"
    assert section['metadata']['doi'] == 'info:doi/' + doi, "DOI field in metadata section did not match!"
    assert section['metadata']['contentType'] == 'application/pdf', "ContentType field in metadata section did not match!"
    assert section['metadata']['extension'] == 'PDF', "Extension field in metadata section did not match!"
    assert section['metadata']['created'] == section['metadata']['lastModified'], "Created & LastModified fields in metadata section did not match!"
    assert section['metadata']['size'] == self.get_size(), "Size field in metadata section did not match!"
    self._verify_created_date(section['metadata'], testStartTime, apiTime)
    print 'OK'
