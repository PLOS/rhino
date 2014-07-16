#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

from Base.IngestibleZipBaseTest import IngestibleZipBaseTest
from Base.ArticlesBaseTest import ArticlesBaseTest
from Base.BaseServiceTest import BaseServiceTest


class CombinedAPITests(IngestibleZipBaseTest, ArticlesBaseTest):

  def test_PS_383_zip_reingestion_publishing_and_reingestion(self):
    '''
    Validate Rhino's PS-383 bug does not resurges.
    '''
    syndications = {
      'CROSSREF': { 'status': 'IN_PROGRESS'}
    }

    self.zipUpload('data/pone.0097823.zip', 'forced')
    self.verify_HTTP_code_is(201)
    self.verify_state_is('ingested')

    self.updateArticle('10.1371/journal.pone.0097823', 'published', syndications)
    self.verify_HTTP_code_is(200)
    self.verify_state_is('published')

    self.zipUpload('data/pone.0097823.zip', 'forced')
    self.verify_HTTP_code_is(201)
    self.verify_state_is('ingested')

if __name__ == '__main__':
    BaseServiceTest._run_tests_randomly()
