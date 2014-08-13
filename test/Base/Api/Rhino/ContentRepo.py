#!/usr/bin/env python2

"""
Base class for Rhino's Content Repo API service tests.
"""

__author__ = 'jkrzemien@plos.org'

from ...Tests.BaseServiceTest import BaseServiceTest
from ...Config import API_BASE_URL

CONTENT_REPO_API = API_BASE_URL + '/repo/%s/%s'


class ContentRepo(BaseServiceTest):

  def serve(self, key, version):

    self.doGet(CONTENT_REPO_API % (key, version), allow_redirects = False)
