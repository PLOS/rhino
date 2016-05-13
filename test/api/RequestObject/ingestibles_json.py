#!/usr/bin/env python2

"""
Base class for Rhino Ingestibles JSON related services
"""

__author__ = 'msingh@plos.org'

from ...Base.base_service_test import BaseServiceTest
from ...Base.Config import API_BASE_URL

INGESTIBLES_API = API_BASE_URL + '/ingestibles'
DEFAULT_HEADERS = {'Accept': 'application/json'}
HEADER = '-H'

OK, CREATED, BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, NOT_ALLOWED = 200, 201, 400, 401, 403, 404, 405

class IngestiblesJson(BaseServiceTest):

  def get_ingestibles(self):
    self.doGet(INGESTIBLES_API, None, headers=DEFAULT_HEADERS)
    self.parse_response_as_json()

  def post_ingestibles(self, parse=True, **kwargs):
    self.doPost(INGESTIBLES_API, data=kwargs, headers=DEFAULT_HEADERS)
    if parse:
      self.parse_response_as_json()

  def verify_get_ingestibles(self, names=None):
    ingestibles = self.parsed.get_json(printvalue=False)
    if names:
      self.assertTrue( bool(ingestibles), "result is not a valid array: %r"%(ingestibles,))
      for name in names:
        self.assertIn(name, ingestibles, "%r not found in result %r"%(name, ingestibles))
