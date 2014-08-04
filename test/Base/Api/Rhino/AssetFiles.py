#!/usr/bin/env python2

"""
Base class for Rhino's Articles related service tests.
"""

__author__ = 'jkrzemien@plos.org'

from ...Tests.BaseServiceTest import BaseServiceTest
from ...Validators.ZIPProcessor import ZIPProcessor
from ...Decorators.Api import deduce_doi, needs
from ...Config import API_BASE_URL

ASSETFILES_API = API_BASE_URL + '/assetfiles/%s/%s.%s'

class AssetFiles(BaseServiceTest):

  def get_asset_for(self, doi, article, extension):

    self.doGet(ASSETFILES_API % (doi, article, extension))

    self.parse_response_as_xml()
