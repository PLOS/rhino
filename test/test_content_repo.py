#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

"""
"""

import re
from Base.Api.Rhino.ContentRepo import ContentRepo


class ContentRepoTest(ContentRepo):
  """
  Test suite for ContentRepo namespace in Rhino's API
  """

  def test_server_happy_path(self):
    """
    GET serve: Redirects call to ContentRepo, looking for a given a KEY/VERSION pair
    """
    key = 'aKey'
    version = 'aVersion'

    # Perform API call
    self.serve(key, version)

    # Assert returned HTTP code is 302 (REDIRECT)
    self.verify_http_code_is(302)

    # Same - Redundant check (?)
    assert self.get_http_response().is_redirect is True, 'Response was supposed to redirect to Content Repo URL, but it did not!'

    # Assert content is an *empty* string
    assert self.get_http_response().content is "", 'Response from service was supposed to be empty, but it was not!'

    headers = self.get_http_response().headers

    # Assert content length is **0** (due to *empty* string content)
    assert headers['content-length'] is '0', 'Response content length was supposed to be 0, but it was not!'

    # Assert redirect location is of the form <http|https>://<domain>/<repoURL>/objects/<contentRepoName>?key=<provided_key>
    assert re.match('https?://[\w./]+/objects/\w+\?key=%s' % key ,headers['location']) is not None, 'Redirect location specified does not match expected one!'

    # Version is not used in invocation to Content Repo ???

if __name__ == '__main__':
  ContentRepo._run_tests_randomly()




