#!/usr/bin/env python3
# -*- coding: utf-8 -*-

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
Base class for Rhino's Ingest API service tests.a
"""
import logging

from .. import resources
from ...Base.Config import dbconfig

__author__ = 'jkrzemien@plos.org; gfilomeno@plos.org; fcabrales@plos.org'

import sys
from test.Base.base_service_test import BaseServiceTest
from test.Base.api import needs
from test.Base.MySQL import MySQL


class Ingestion(BaseServiceTest):

    @needs('parsed', 'parse_response_as_json()')
    def verify_article(self, not_scaped_article_doi):
        """
        Validate ingestion with Article table
        :param not_scaped_article_doi: String. Such as '10.1371/journal.pone.0155391'
        :return: None
        """
        logging.info(not_scaped_article_doi)
        article_id = self.get_article_id_sql_doi(not_scaped_article_doi)
        # # Verify uploaded DOI against the one stored in DB
        self.verify_ingestion_text_expected_only(not_scaped_article_doi, 'doi')
        # Verify article title stored in DB
        article_title = self.get_article_sql_archiveName(article_id)
        self.verify_ingestion_text_expected_only(article_title[0], 'title')
        # Verify article type stored in DB
        article_type = self.get_article_sql_type(article_id)
        self.verify_ingestion_text_expected_only(article_type[0], 'articleType')
        # Verify article publication date in DB
        article_pubdate = self.get_article_sql_pubdate(article_id)
        self.verify_ingestion_text_expected_only(str(article_pubdate[0]), 'publicationDate')

    @needs('parsed', 'parse_response_as_json()')
    def verify_journals(self, not_scaped_article_doi):
        """
        Validate ingestion with articlePublishedJournals  table
        :param not_scaped_article_doi: String. Such as '10.1371/journal.pone.0155391'
        :return: None
        """

        article_id = self.get_article_id_sql_doi(not_scaped_article_doi)
        logging.info('Verify Journals')
        article_id = self.get_article_id_sql_doi(not_scaped_article_doi)
        journals = self.get_journals_sql_archiveName(article_id)
        journals_json = self.parsed.get_attribute('journal')
        self.verify_array(journals, journals_json)
        for journal in journals:
            journal_name = str(journal[0])
            self.verify_ingestion_text(journals_json['journalKey'], journal[0],
                                       journal_name + '.journalKey')
            self.verify_ingestion_text(journals_json['eIssn'], journal[1], journal_name + '.eIssn')
            self.verify_ingestion_text(journals_json['title'], journal[2], journal_name + '.title')

    def verify_article_figures(self, not_scaped_article_doi):
        """
        Validate ingestion's figures with Assert table
        :param not_scaped_article_doi: String. Such as '10.1371/journal.pone.0155391'
        :return: None
        """
        logging.info('Verify Article\'s figures')
        article_id = self.get_article_id_sql_doi(not_scaped_article_doi)
        self.verify_article_assets(article_id, 'assetsLinkedFromManuscript')

    def verify_article_assets(self, article_id, assets_json_name):
        """
        Executes SQL statement against ambra articleItem table and compares to rhino get article
          json response
        :param article_id: String. Such as '55391'
        :param assets_json_name: String. Such as 'assetsLinkedFromManuscript'
        :return: none
        """
        assets = self.get_asset_figures_graphics(article_id)
        assets_json = self.parsed.get_attribute(assets_json_name)
        i = 0
        for asset in assets:
            self.verify_ingestion_text(assets_json[i]['doi'], asset[0], 'doi')
            i += 1

    @needs('parsed', 'parse_response_as_json()')
    def verify_ingestion_text_expected_only(self, expected, attribute):
        actual = self.parsed.get_attribute(attribute)
        if isinstance(actual, str) and sys.version_info[0] >= 3:
            actual = bytes(actual, 'utf-8')
        elif sys.version_info[0] == 2 and isinstance(actual, unicode):
            actual = bytearray(actual, 'utf-8')
        if isinstance(expected, str) and sys.version_info[0] >= 3:
            # if sys.version_info[0]>=3:
            expected = bytes(expected, 'utf-8')
        elif sys.version_info[0] == 2 and isinstance(expected, unicode):
            expected = bytearray(expected, 'utf-8')
        assert actual == expected, ('%s is not correct! actual: %s expected: %s' % (
            attribute, actual, expected))

    @needs('parsed', 'parse_response_as_json()')
    def verify_ingestion_text(self, actual_results, expected_results, attribute):
        assert actual_results.encode('utf-8') == expected_results, \
            ('%s is not correct! actual: %s expected: %s' % (
                attribute, actual_results, expected_results))

    @needs('parsed', 'parse_response_as_json()')
    def verify_ingestion_attribute(self, actual_results, expected_results, attribute):
        assert actual_results == expected_results, \
            ('%s is not correct! actual: %s expected: %s' % (
                attribute, actual_results, expected_results))

    def verify_array(self, actual_array, expected_array):
        self.assertIsNotNone(actual_array)
        self.assertIsNotNone(expected_array)

    def get_article_id_sql_doi(self, not_scape_doi):
        """
        Executes SQL statement against ambra article table to get article id
        :param not_scapted_doi: String. Such as '10.1371/journal.pone.0155391'
        :return: Int Article id
        """
        current_articles_id = MySQL().query('SELECT articleId FROM article WHERE doi = %s',
                                            [not_scape_doi])
        return current_articles_id[0][0]

    def delete_article_sql_doi(self, not_scape_doi):
        """
        Executes SQL statement which deletes article from ambra db
        :param not_scapted_doi: String. Such as '10.1371/journal.pone.0155391'
        :return: none
        """
        current_articles_id = self.get_article_id_sql_doi(not_scape_doi)
        logging.info('Call sql stored procedure: CALL migrate_article_rollback({0}, '
                     'connection_timeout: {1})'
                     .format(current_articles_id, dbconfig['connection_timeout']))
        try:
            MySQL().modify('CALL migrate_article_rollback(%s)', [current_articles_id])
        except IOError as err:
            logging.error('Call sql stored procedure: CALL migrate_article_rollback({0}, '
                          'connection_timeout: {1})'
                          .format(current_articles_id, dbconfig['connection_timeout']))

        return self

    def get_article_sql_archiveName(self, article_id):
        """
        Executes SQL statement against ambra articleIngestion table to get article title
        :param article_id: String. Such as '55391'
        :return: String article_title
        """
        article_title = MySQL().query('SELECT title FROM articleIngestion WHERE articleId = %s',
                                      [article_id])
        return article_title[0]

    def get_article_sql_type(self, article_id):
        """
        Executes SQL statement against ambra articleIngestion table to get article type
        :param article_id: String. Such as '55391'
        :return: String article_type
        """
        article_type = MySQL().query(
                'SELECT articleType FROM articleIngestion WHERE articleId = %s', [article_id])
        return article_type[0]

    def get_article_sql_pubdate(self, article_id):
        """
        Executes SQL statement against ambra articleIngestion table to get article publication date
        :param article_id: String. Such as '55391'
        :return: String article_publication_date
        """
        article_publication_date = MySQL().query(
                'SELECT publicationDate FROM articleIngestion WHERE articleId = %s', [article_id])
        return article_publication_date[0]

    def get_journals_sql_archiveName(self, article_id):
        """
        Executes SQL statement joins ambra journal and articleIngestion table to get journalKey,
        eIssn and title
        :param article_id: String. Such as '55391'
        :return: List bytearray  journalKey,eIssn,title
        """
        journals = MySQL().query(
                'SELECT j.journalKey, j.eIssn, j.title '
                'FROM journal AS j JOIN articleIngestion aj ON  j.journalID = aj.journalID '
                'JOIN articleIngestion as a ON aj.articleID = a.articleID '
                'WHERE a.articleid = %s ORDER BY j.journalID', [article_id])
        return journals

    def get_asset_figures_graphics(self, article_id):
        """
        Executes SQL statement against ambra articleItem table to get article assets given
        ingestion_id
        :param article_id: String. Such as '55391'
        :return: List tuples assets
        """
        ingestion_id = MySQL().query(
                'SELECT ingestionId FROM articleIngestion WHERE articleId = %s', [article_id])
        assets = MySQL().query('SELECT doi FROM articleItem '
                               'WHERE ingestionId = %s and articleItemType = "figure"'
                               'ORDER BY doi', [ingestion_id[0][0]])
        return assets

    def get_article_status(self, status):
        return {
            1: resources.INGESTED
        }[status]
