#!/usr/bin/env python2

'''
Class for accessing MySQL databases and be able to run queries against it, retrieving results
and/or performing modifications.

Python's MySQL connector can be installed via the following command:

  sudo pip install mysql-connector-python

'''

__author__ = 'jkrzemien@plos.org'

from mysql.connector.pooling import MySQLConnectionPool
from contextlib import closing
import Config


class Database(object):

  def __init__(self):
    self._cnxpool = MySQLConnectionPool(pool_name="daPool", pool_size=3, **Config.dbconfig)

  def _getConnection(self):
    return self._cnxpool.get_connection()

  def query(self, query, queryArgsTuple=None):
    cnx = self._getConnection()

    with closing(cnx.cursor()) as cursor:
      cursor.execute(query, queryArgsTuple)
      results = cursor.fetchall()

    cnx.close()

    return results

  def modify(self, query, queryArgsTuple=None):
    cnx = self._getConnection()

    with closing(cnx.cursor()) as cursor:
      cursor.execute(query, queryArgsTuple)
      cnx.commit()

    cnx.close()

