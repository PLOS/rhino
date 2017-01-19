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
Base class for Rhino Ingestibles JSON related services
"""

__author__ = 'msingh@plos.org'

from ...Base.Config import API_BASE_URL
from ingestion_json import Ingestion

INGESTIBLES_API = API_BASE_URL + '/ingestibles'
DEFAULT_HEADERS = {'Accept': 'application/json'}
HEADER = '-H'

OK, CREATED, BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, NOT_ALLOWED = 200, 201, 400, 401, 403, 404, 405

class IngestiblesJson(Ingestion):

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
