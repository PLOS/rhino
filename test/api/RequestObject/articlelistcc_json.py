#!/usr/bin/env python2

'''
Base class for Article list crud controller  JSON related services
'''

__author__ = 'fcabrales@plos.org'

from ...Base.base_service_test import BaseServiceTest
from ..resources import *
import json
from ...Base.MySQL import MySQL

ARTICLE_LIST_API = API_BASE_URL + '/v1/lists/'

class ArticlesListJSON(BaseServiceTest):

  def add_article_list(self):
    '''
    Calls rhino POST article list API with parameters
    :param articlelistbody: body of articlelist
    :return:JSON response
    '''
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
