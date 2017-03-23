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

__author__ = 'fcabrales@plos.org'


"""
This test case validates Rhino's article repack service.
"""

from ..api.RequestObject.ingestiblec_json import IngestibleJSON
import resources

class ZipIngestibleTest(IngestibleJSON):

  def setUp(self):
    self.already_done = 0

  def tearDown(self):
    """
    Purge all objects and collections created in the test case
    """
    if self.already_done > 0: return

  def test_ingestible(self):
    """
    GET ingestibles
    """
    print('\nTesting GET  ingestibles/\n')
    # Invoke ingestibles API
    self.get_ingestible(resources.ARTICLE_DOI)

if __name__ == '__main__':
  IngestibleJSON._run_tests_randomly()
