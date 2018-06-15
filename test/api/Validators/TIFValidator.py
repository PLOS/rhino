#!/usr/bin/env python3
# -*- coding: utf-8 -*-
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


"""
This class loads up the name and data for a TIF file from the ingestion ZIP file, along with the
XML file in order to be used for comparison between data in ZIP and API's responses.
"""

from datetime import datetime
import logging

from .AbstractValidator import AbstractValidator

__author__ = 'jkrzemien@plos.org'


class TIFValidator(AbstractValidator):

    def __init__(self, name, data, xml):
        super(TIFValidator, self).__init__(data)
        self._name = name
        self._xml = xml
        self.DOI_HEADER = 'info:doi/'
        self.DOI_PREFFIX = '10.1371/journal.'
        self.MIME = 'image/tiff'
        self.EXT = 'TIF'

    def metadata(self, section, doi, test_start_time, api_time):
        logging.info('Validating Graphics metadata section in Response...')

        assert section is not None, "Graphics section in response is NULL!"
        assert section['doi'] == doi, "DOI field in Graphics section did not match!"

        matched_xml_file = self._xml.find(".//fig/label[contains(text(),{0!r})]"
                                          .format(section['title']))
        assert matched_xml_file is not None, "Title field in Graphics section did not match!"

        matched_xml_file = self._xml.find(
                ".//fig/caption/p[contains(text(),{0!r})]".format(section['description']))
        assert matched_xml_file is not None, "Description field in Graphics section did not match!"

        xpath = ".//{0}/*[@xlink:href={1!r}]".format(
            section['contextElement'], section['original']['metadata']['doi'])
        matched_xml_file = self._xml.find(xpath)
        assert matched_xml_file is not None, \
            "{0} field in Graphics section did not match!".format(xpath)

        file_name = self.DOI_PREFFIX + self._name

        assert section['original']['file'].lower() == file_name.lower(), \
            "File field in Graphics section did not match!"
        assert section['original']['metadata']['doi'] == doi, \
            "DOI field in Graphics section did not match!"
        assert section['original']['metadata']['contentType'] == self.MIME, \
            "ContentType field in Graphics section did not match!"
        assert section['original']['metadata']['extension'] == self.EXT, \
            "Extension field in Graphics section did not match!"
        assert section['original']['metadata']['created'] ==\
            section['original']['metadata']['lastModified'], \
            "Created field in Graphics section did not match!"
        assert section['original']['metadata']['size'] == self.get_size(), \
            "Size field in Graphics section did not match!"
        self._verify_created_date(section['original']['metadata'], test_start_time, api_time)
        logging.info('OK')

    def _verify_created_date(self, section, test_start_time, api_time):
        # Some dates (PDF section) seem to include millis too, double check for possible bug?
        section_date = datetime.strptime(section['created'], '%Y-%m-%dT%H:%M:%S.%fZ')
        deltaTime = section_date - test_start_time
        assert deltaTime.total_seconds() > 0, \
            "Created field in metadata section should be greater than test start time!"
        # Next validation is not working properly because there seems to be a difference of
        # around 7 hours between my box and one-leo.plosjournals.org environment (?)
        # assert apiTime > deltaTime.total_seconds(), "API invocation time should be greater 
        # than diff between Created field in metadata section & test start time!"
