#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'


"""
"""

from Base.Api.Rhino.Ingestion import ZIPIngestion
from Base.Api.Rhino.AssetFiles import AssetFiles



class IngestiblesTest(AssetFiles, ZIPIngestion):

  """
  Test suite for AssetFiles namespace in Rhino's API
  """

  def test_retrieve_ingested_file_happy_path(self):
    """
    GET assetfiles: Retrieve XML article file from a ZIP ingestion
    """
    doi = '10.1371'
    journal = 'journal.pone.0097823'

    self.zipUpload('pone.0097823.zip', 'forced')
    self.verify_http_code_is(201)

    self.get_asset_for(doi, journal, 'xml')
    self.verify_http_code_is(200)

    assert self.parsed is not None, 'XML was not parsed, it is NULL'
    doi_nodes = self.parsed.get_doi()
    assert doi_nodes is not None, 'XML did not contain any DOI nodes'
    for node in doi_nodes:
      expected = '%s/%s' % (doi, journal)
      assert node.content == expected, 'Expected DOI is %s, but %s found' % (expected, node.content)


if __name__ == '__main__':
    AssetFiles._run_tests_randomly()




