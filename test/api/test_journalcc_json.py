#!/usr/bin/env python2

__author__ = 'jgray@plos.org'

'''
Test cases for Rhino Journal Crud Controller requests.
'''
from ..api.RequestObject.journalcc_json import JournalCCJson


class GetJournals(JournalCCJson):

  def test_journals(self):
    """
    Get Journalss API call
    """
    self.get_journals()
    self.verify_journals()

if __name__ == '__main__':
    JournalCCJson._run_tests_randomly()
