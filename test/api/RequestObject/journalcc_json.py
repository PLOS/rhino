#!/usr/bin/env python2

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
EXPECTED_KEYS = [u'PLoSBiology', u'PLoSPathogens', u'PLoSDefault', u'PLoSMedicine', u'PLoSNTD', u'PLoSCompBiol', u'PLoSCollections', u'PLoSClinicalTrials', u'PLoSGenetics', u'PLoSONE']


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
