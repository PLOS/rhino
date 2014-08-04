#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

from Base.Api.Rhino.Articles import Articles
from Base.Api.Rhino.Ingestion import ZIPIngestion


class CombinedAPITests(ZIPIngestion, Articles):

  def test_PS_383_zip_reingestion_publishing_and_reingestion(self):
    """
    Validate Rhino's PS-383 bug does not re-emerges.
    """

    # Prepare API arguments
    syndications = {
      'CROSSREF': { 'status': 'IN_PROGRESS'}
    }

    # Invoke API
    self.zipUpload('pone.0097823.zip', 'forced')
    self.parse_response_as_json()

    # Validate it worked
    self.verify_http_code_is(201)
    self.verify_state_is('ingested')

    # Invoke API
    self.updateArticle('10.1371/journal.pone.0097823', 'published', syndications)

    # Validate it worked
    self.verify_http_code_is(200)
    self.verify_state_is('published')

    # Invoke API
    self.zipUpload('pone.0097823.zip', 'forced')

    # Validate it worked
    self.verify_http_code_is(201)
    self.verify_state_is('ingested')

if __name__ == '__main__':
    ZIPIngestion._run_tests_randomly()
