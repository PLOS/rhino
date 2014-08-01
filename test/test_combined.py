#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

from Base.Api.Ingestion import ZIPIngestion
from Base.Api.Articles import Articles

class CombinedAPITests(ZIPIngestion, Articles):

  def test_PS_383_zip_reingestion_publishing_and_reingestion(self):
    """
    Validate Rhino's PS-383 bug does not resurges.
    """
    syndications = {
      'CROSSREF': { 'status': 'IN_PROGRESS'}
    }

    self.zipUpload('pone.0097823.zip', 'forced')
    self.verify_http_code_is(201)
    self.verify_state_is('ingested')

    self.updateArticle('10.1371/journal.pone.0097823', 'published', syndications)
    self.verify_http_code_is(200)
    self.verify_state_is('published')

    self.zipUpload('pone.0097823.zip', 'forced')
    self.verify_http_code_is(201)
    self.verify_state_is('ingested')

if __name__ == '__main__':
    ZIPIngestion._run_tests_randomly()
