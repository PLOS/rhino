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
        assert actual_array is not None
        assert expected_array is not None

    def get_article_id_sql_doi(self, not_scape_doi):
        """
        Executes SQL statement against ambra article table to get article id
        :param not_scapted_doi: String. Such as '10.1371/journal.pone.0155391'
        :return: Int Article id
        """
        current_articles_id = MySQL().query('SELECT articleId FROM article WHERE doi = %s',
                                            [not_scape_doi])
        return 0 if not current_articles_id else current_articles_id[0][0]

    def delete_article_sql_doi(self, not_scape_doi, ingestion_number=1):
        """
        Executes SQL statement which deletes article from ambra db
        :param not_scapted_doi: String. Such as '10.1371/journal.pone.0155391'
        :return: none
        """
        current_articles_id = self.get_article_id_sql_doi(not_scape_doi)
        logging.info('Call sql stored procedure: CALL migrate_article_rollback({0}, '
                     'connection_timeout: {1})'
                     .format(current_articles_id, dbconfig['connection_timeout']))
        if ingestion_number == 1:
            try:
                MySQL().modify('CALL migrate_article_rollback(%s)', [current_articles_id])
            except:
                logging.error('Call sql stored procedure: CALL migrate_article_rollback({0}, '
                              'connection_timeout: {1})'
                              .format(current_articles_id, dbconfig['connection_timeout']))
        else:
            ingestion_id = self.get_article_sql_ingestion_id(current_articles_id, ingestion_number)
            self.delete_article_files(ingestion_id)
            self.update_constraint(ingestion_id)
            self.delete_article_items(ingestion_id)
            self.delete_ingestion(ingestion_id)
        return self

    def clean_article_sql_doi(self, not_scape_doi):
        """
        Executes SQL statement which deletes article from ambra db
        :param not_scapted_doi: String. Such as '10.1371/journal.pone.0155391'
        :return: none
        """
        current_article_id = self.get_article_id_sql_doi(not_scape_doi)
        if not current_article_id:
            return self

        ingestion_id_list = self.get_article_sql_ingestions(current_article_id)
        if ingestion_id_list:
            logging.info('Found previous article ingestion(s). Cleaning... ')
            if len(ingestion_id_list) == 1:
                try:
                    MySQL().modify('CALL migrate_article_rollback(%s)', [current_article_id])
                except:
                    logging.error('Call sql stored procedure: CALL migrate_article_rollback({0}, '
                                  'connection_timeout: {1})'
                                  .format(current_article_id, dbconfig['connection_timeout']))
            else:
                self.delete_article_comment_flag(current_article_id)
                self.delete_article_comments(current_article_id)
                self.delete_article_relationship(current_article_id)
                self.delete_article_category(current_article_id)
                self.delete_article_from_list_join(current_article_id)
                self.delete_article_from_list(current_article_id)

                for ingestion_id in ingestion_id_list:
                    self.delete_article_files(ingestion_id)
                    self.update_constraint(ingestion_id)
                    self.delete_article_items(ingestion_id)
                    self.delete_syndications(ingestion_id)
                    self.delete_article_revision(ingestion_id)
                    self.delete_ingestion(ingestion_id)
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
                'SELECT articleType '
                'FROM articleIngestion '
                'WHERE articleId = %s and ingestionNumber = %s', [article_id,
                                                                  self.ingestion_number])
        return article_type[0]

    def get_article_sql_pubdate(self, article_id):
        """
        Executes SQL statement against ambra articleIngestion table to get article publication date
        :param article_id: String. Such as '55391'
        :return: String article_publication_date
        """
        article_publication_date = MySQL().query(
                'SELECT publicationDate '
                'FROM articleIngestion '
                'WHERE articleId = %s and ingestionNumber = %s', [article_id,
                                                                  self.ingestion_number])
        return article_publication_date[0]

    def get_article_sql_ingestion_id(self, article_id, ingestion_number):
        """
        Executes SQL statement against ambra articleIngestion table to get article ingestion id
        for specific ingestion number
        :param article_id: String. Such as '55391'
        :param ingestion number
        :return: String ingestion id
        """
        ingestion_id = MySQL().query(
                'SELECT ingestionId FROM articleIngestion WHERE articleId = %s and '
                'ingestionNumber = %s', [article_id, ingestion_number])
        return ingestion_id[0][0]

    def get_article_sql_ingestions(self, article_id):
        """
        Executes SQL statement against ambra articleIngestion table to get all article ingestion
        id's
        :param article_id: String. Such as '55391'
        :return: String ingestion id
        """
        ingestion_id = MySQL().query(
                'SELECT ingestionId FROM articleIngestion WHERE articleId = {0!r}'
                .format(article_id))
        ingestion_id_list = [i[0] for i in ingestion_id]
        return ingestion_id_list

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
        ingestion_id = self.get_article_sql_ingestion_id(article_id, self.ingestion_number)
        assets = MySQL().query('SELECT doi FROM articleItem '
                               'WHERE ingestionId = %s and articleItemType = "figure"'
                               'ORDER BY doi', [ingestion_id,])
        return assets

    def get_article_status(self, status):
        return {
            1: resources.INGESTED
        }[status]

    def delete_article_files(self, ingestion_id):
        """
        Runs a delete sql statements to remove any created files from articleFile
        :param ingestion_id: ingestion_id.
        """
        MySQL().modify(
                "DELETE "
                "FROM articleFile "
                "WHERE ingestionId = {0!r}".format(ingestion_id))

    def delete_article_items(self, ingestion_id):
        """
        Runs a delete sql statements to remove any created items from articleItem
        :param ingestion_id: ingestion_id.
        """
        MySQL().modify(
                "DELETE "
                "FROM articleItem "
                "WHERE ingestionId = {0!r}".format(ingestion_id))

    def update_constraint(self, ingestion_id):
        """

        :param ingestion_id:
        :return:
        """
        MySQL().modify(
                "UPDATE articleIngestion "
                "SET strikingImageItemId = NULL "
                "WHERE ingestionId = {0!r}".format(ingestion_id))

    def delete_syndications(self, ingestion_id):
        """
        Runs a delete sql statements to remove any syndication
        :param ingestion_id: ingestion_id.
        """
        MySQL().modify(
                "DELETE "
                "FROM syndication "
                "WHERE revisionId IN "
                "(SELECT revisionId FROM articleRevision WHERE ingestionId = {0!r})"
                .format(ingestion_id))

    def delete_article_revision(self, ingestion_id):
        """
        Runs a delete sql statements to remove any article revision
        :param ingestion_id: ingestion_id.
        """
        MySQL().modify(
                "DELETE "
                "FROM articleRevision "
                "WHERE ingestionId = {0!r}".format(ingestion_id))

    def delete_ingestion(self, ingestion_id):
        """
        Runs a delete sql statements to remove any created ingestion from articleIngestion
        :param ingestion_id: ingestion_id.
        """
        MySQL().modify(
                "DELETE "
                "FROM articleIngestion "
                "WHERE ingestionId = {0!r}".format(ingestion_id))

    def delete_article_comment_flag(self, article_id):
        """
        Runs a delete sql statements to remove any article comment flags
        :param article_id: String. Such as '55391'
        """
        MySQL().modify(
                "DELETE "
                "FROM commentFlag "
                "WHERE commentId in "
                "(SELECT commentId FROM comment "
                "WHERE articleId = {0!r})".format(article_id))

    def delete_article_comments(self, article_id):
        """
        Runs a delete sql statements to remove any article comments
        :param article_id: String. Such as '55391'
        """
        MySQL().modify(
                "DELETE "
                "FROM comment "
                "WHERE articleId = {0!r}".format(article_id))

    def delete_article_relationship(self, article_id):
        """
        Runs a delete sql statements to remove any article relationship
        :param article_id: String. Such as '55391'
        """
        MySQL().modify(
                "DELETE "
                "FROM articleRelationship "
                "WHERE sourceArticleId = {0!r} OR targetArticleId = {0!r}"
                .format(article_id))

    def delete_article_category(self, article_id):
        """
        Runs a delete sql statements to remove any article categories
        :param article_id: String. Such as '55391'
        """
        MySQL().modify(
                "DELETE "
                "FROM articleCategoryAssignment "
                "WHERE articleId = {0!r}".format(article_id))

    def delete_article_from_list(self, article_id):
        """
        Runs a delete sql statements to remove the article from article list
        :param article_id: String. Such as '55391'
        """
        MySQL().modify(
                "DELETE "
                "FROM issueArticleList "
                "WHERE articleId = {0!r}".format(article_id))

    def delete_article_from_list_join(self, article_id):
        """
        Runs a delete sql statements to remove article from articleListJoinTable
        :param article_id: String. Such as '55391'
        """
        MySQL().modify(
                "DELETE "
                "FROM articleListJoinTable "
                "WHERE articleId = {0!r}".format(article_id))
