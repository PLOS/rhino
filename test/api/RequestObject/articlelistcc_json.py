#!/usr/bin/env python2

"""
Base class for Article list crud controller  JSON related services
"""

__author__ = 'fcabrales@plos.org'

from ...Base.base_service_test import BaseServiceTest
from ..resources import *
import json
from ...Base.MySQL import MySQL

ARTICLE_LIST_API = API_BASE_URL + '/lists/'
ARTICLE_LIST_PATCH_API = ARTICLE_LIST_API + 'collection/PLoSCollections/'

class ArticlesListJSON(BaseServiceTest):

  def add_article_list(self, expected_response_code):
    """
    Calls rhino POST article list API with parameters
    """
    daData = json.dumps({
               "type": "collection",
               "journal": "PLoSCollections",
               "key": "rhino-cell-collection",
               "title": "Rhino cell collection - The Cell Collection",
               "articleDois": [
                   "10.1371/journal.pone.0068293",
                   "10.1371/journal.pone.0027064",
                   "10.1371/journal.pone.0013780",
                   "10.1371/journal.pone.0012369"
                   ]
           })
    self.doPost(ARTICLE_LIST_API, daData)
    self.verify_http_code_is(expected_response_code)

  def patch_article_list(self, expected_response_code, article_list_key, use_bogus_data=False):
    """
    Calls rhino PATCH article list API with parameters
    """
    if use_bogus_data:
      data = json.dumps({
        "blitle": "Rhino cell collection - The Next Generation",
        "articleDois": ["Garbagio!"]
      })
    else:
      data = json.dumps({
          "title": "Rhino cell collection - The Next Generation",
          "articleDois": [
              "10.1371/journal.pone.0009957",
              "10.1371/journal.pone.0010363",
          ]
      })
    self.doPatch(ARTICLE_LIST_PATCH_API + article_list_key, data)
    self.verify_http_code_is(expected_response_code)

  def delete_lists_articlelistjointable(self, list_name):
    """
    Runs a delete sql statements to remove any created lists from articleListJoinTable
    :param name: list_name.
    """
    MySQL().modify("DELETE articleListJoinTable FROM articleListJoinTable  INNER JOIN articleList ON articleListJoinTable.articleListID = articleList.articleListID WHERE articleList.listKey=" "'" + str(list_name)+ "'")

  def delete_lists_articlelist(self, list_name):
    """
    Runs a delete sql statements to remove any created lists from articleList
    :param name: list_name.
    """
    MySQL().modify("DELETE articleList FROM articleList WHERE articleList.listKey = " "'" + str(list_name)+ "'")
