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

"""
Base class for Rhino Journal JSON related services
"""

__author__ = 'jgray@plos.org'

from ...Base.base_service_test import BaseServiceTest
from ...Base.Config import API_BASE_URL
from ...Base.api import needs

JOURNALS_API = API_BASE_URL + '/journals'
DEFAULT_HEADERS = {'Accept': 'application/json'}
HEADER = '-H'
EXPECTED_KEYS = [u'PLoSMedicine', u'PLoSONE', u'PLoSGenetics', u'PLoSCompBiol', u'PLoSCollections', u'PLoSDefault', u'PLoSNTD', u'PLoSBiology', u'PLoSClinicalTrials', u'PLoSPathogens']


class JournalCCJson(BaseServiceTest):

  def get_journals(self):
    """
    Calls Rhino API to get journal list
    :param
    :return:JSON response
    """
    header = {'header': HEADER}
    self.doGet('%s' % JOURNALS_API, header, DEFAULT_HEADERS)
    self.parse_response_as_json()

  @needs('parsed', 'parse_response_as_json()')
  def verify_journals(self):
    """
    Verifies a valid response
    :param
    :return: Journal List + OK
    """
    print ('Validating journals...'),
    actual_keys = self.parsed.get_journalKey()
    assert actual_keys == EXPECTED_KEYS, 'Journals keys did not match: Actual Keys %s != Expected Keys %s' % (actual_keys, EXPECTED_KEYS)
    print ('OK')
