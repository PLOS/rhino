#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'


"""
This test case validates Rhino's convenience zipUpload Tests for ZIP ingestion.

Notes:

* For Data-Driven Testing (DDT) you can use ddt, available via: pip install ddt

Decorate your test class with @ddt and @data for your test methods, you will also need to pass an
extra argument to the test method.

* Using Nose's parameterized feature is not recommended since Nose doesn't play nice
with subclasses.

* Still need to take a look @ https://code.google.com/p/mogilefs/wiki/Clients
for MogileFS's Python client implementations.
"""

from Base.Api.Rhino.Ingestion import Ingestibles, ZIPIngestion


class IngestiblesTest(Ingestibles, ZIPIngestion):

  """
  Test suite for Ingestibles namespace in Rhino's API
  """

  def test_list_ingestibles_happy_path(self):
    """
    GET ingestibles: list ingestibles
    """

    self.list_ingestibles()
    self.verify_http_code_is(200)
    self.parse_response_as_json()
    assert self.parsed.get_json() == []

  def test_list_ingestibles_after_ingestion(self):
    """
    GET ingestibles: list ingestibles does NOT come from /zips/ API.

    According to documentation:
    /ingestibles/ use designated source and destination directories.
    Zip files are placed in the source directory outside of this API, and upon successful ingest,
    they are moved to the destination directory by rhino.
    This is the way the current admin interface works.
    """

    self.list_ingestibles()
    self.verify_http_code_is(200)
    self.parse_response_as_json()
    assert self.parsed.get_json() == []

    self.zipUpload('pone.0097823.zip', 'forced')
    self.verify_http_code_is(201)

    self.list_ingestibles()
    self.verify_http_code_is(200)
    assert self.parsed.get_json() == []

  def test_ingest_archive_error_due_to_file_not_present_not_forced(self):
    """
    POST ingestibles: Throws an error due to file not present (not forced)
    """

    self.ingest_archive('afile.txt')
    self.verify_http_code_is(405)
    assert self.get_http_response().text == 'Could not find ingestible archive for: afile.txt\n'

  def test_ingest_archive_error_due_to_file_not_present_forced(self):
    """
    POST ingestibles: Throws an error due to file not present (forced)
    """

    self.ingest_archive('afile.txt', 'forcing you!')
    self.verify_http_code_is(405)
    assert self.get_http_response().text == 'Could not find ingestible archive for: afile.txt\n'


if __name__ == '__main__':
    Ingestibles._run_tests_randomly()




