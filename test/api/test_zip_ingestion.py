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
This test case validates Rhino's convenience zipUpload Tests for ZIP ingestion.
"""
import logging
import pytest

from ..api import resources
from .RequestObject.zip_ingestion import ZIPIngestionJson
from .RequestObject.memory_zip import MemoryZipJSON
from ..api.resources import RA_DOI, CREATED, RELATED_ARTICLE_DOI, NOT_SCAPE_RELATED_ARTICLE_DOI, \
    RELATED_ARTICLE_BUCKET_NAME, OK, NOT_FOUND, NOT_SCAPE_PREPRINT_ARTICLE_DOI, PP_DOI, \
    PREPRINT_ARTICLE_BUCKET_NAME, PREPRINT_ARTICLE_DOI

__author__ = 'jkrzemien@plos.org; gfilomeno@plos.org'


class TestZipIngestion(ZIPIngestionJson, MemoryZipJSON):

    @pytest.fixture(scope="module", name='setup')
    def set_up(self, request):
        self.already_done = 0

        def tear_down():
            """
            Purge all objects and collections created in the test case
            """
            try:
                if self.already_done > 0:
                    return
            except:
                pass

        request.addfinalizer(tear_down)

    @pytest.mark.usefixtures("setup")
    def test_zip_ingestion_related_article(self):
        """
        POST zips: Forced ingestion of ZIP archive
        """
        logging.info('\nTesting POST zips for related article/\n')
        # if article exists, clean all previous ingestions
        self.clean_article_sql_doi(NOT_SCAPE_RELATED_ARTICLE_DOI)
        # Invoke ZIP API
        zip_file = self.create_ingestible(RA_DOI, 'RelatedArticle/')
        response = self.post_ingestible_zip(zip_file, RELATED_ARTICLE_BUCKET_NAME)
        self.ingestion_number = self.parsed.get_attribute("ingestionNumber")
        # Validate HTTP code in the response is 201 (CREATED)
        self.verify_http_code_is(response, CREATED)
        # Validate response with database tables
        self.verify_zip_ingestion(NOT_SCAPE_RELATED_ARTICLE_DOI)
        self.delete_test_article(RELATED_ARTICLE_DOI,
                                 NOT_SCAPE_RELATED_ARTICLE_DOI,
                                 RELATED_ARTICLE_BUCKET_NAME)

    @pytest.mark.usefixtures("setup")
    def test_zip_ingestion_related_article_no_bucket(self):
        """
        POST zips: Forced ingestion of ZIP archive
        """
        logging.info('\nTesting POST zips for related article no bucket/\n')
        # if article exists, clean all previous ingestions
        self.clean_article_sql_doi(NOT_SCAPE_RELATED_ARTICLE_DOI)
        # Invoke ZIP API
        zip_file = self.create_ingestible(RA_DOI, 'RelatedArticle/')
        response = self.post_ingestible_zip(zip_file)
        self.ingestion_number = self.parsed.get_attribute("ingestionNumber")
        # Validate HTTP code in the response is 201 (CREATED)
        self.verify_http_code_is(response, CREATED)
        # Validate response with database tables
        self.verify_zip_ingestion(NOT_SCAPE_RELATED_ARTICLE_DOI)
        self.delete_test_article(RELATED_ARTICLE_DOI,
                                 NOT_SCAPE_RELATED_ARTICLE_DOI,
                                 RELATED_ARTICLE_BUCKET_NAME)

    @pytest.mark.usefixtures("setup")
    def test_zip_ingestion_preprint_article(self):
        """
        POST zips: Forced ingestion of ZIP archive
        """
        logging.info('\nTesting POST zips for preprint article/\n')
        # Invoke ZIP API
        zip_file = self.create_ingestible(PP_DOI, 'PrePrint/')
        response = self.post_ingestible_zip(zip_file, PREPRINT_ARTICLE_BUCKET_NAME)
        self.ingestion_number = self.parsed.get_attribute("ingestionNumber")
        # Validate HTTP code in the response is 201 (CREATED)
        self.verify_http_code_is(response, CREATED)
        # Validate response with database tables
        self.verify_zip_ingestion(NOT_SCAPE_PREPRINT_ARTICLE_DOI)
        self.delete_test_article(PREPRINT_ARTICLE_DOI,
                                 NOT_SCAPE_PREPRINT_ARTICLE_DOI,
                                 PREPRINT_ARTICLE_BUCKET_NAME)

    @pytest.mark.usefixtures("setup")
    def test_zip_ingestion_without_file(self):
        """
        POST zips: Try to ingest of ZIP archive without file name
        """
        logging.info('\nTesting POST zips/ without parameters\n')
        # Ingest a ZIP file
        try:
            self.already_done = 1
            self.post_ingestible_zip(None)
        except:
            pass

    def verify_zip_ingestion(self, not_scaped_article_doi):
        """
        Verifies article, journal and figure inforamtion from ambra db
        :param not_scapted_article_doi: String. Such as '10.1371/journal.pone.0155391'
        :return: none
        """
        # All below verifications will be fix with https://developer.plos.org/jira/browse/DPRO-3259
        # Validate response with Article table
        self.verify_article(not_scaped_article_doi)
        # Validate response with Journal table
        self.verify_journals(not_scaped_article_doi)
        # Validate article figures
        self.verify_article_figures(not_scaped_article_doi)

    def delete_test_article(self, article_doi, not_scaped_article_doi, bucket_name):
        """
        Gets article information for rhino then proceeds to delete article records from ambra db
        and content repo database
        :param article_doi: String. Such as '10.1371++journal.pone.0170224'
        :param not_scapted_article_doi: String. Such as '10.1371/journal.pone.0155391'
        :param bucket_name: String. Such as 'preprint'
        :return: None
        """
        try:
            response = self.get_article(article_doi)
            status_code = response.status_code
            if status_code == OK:
                self.delete_article_sql_doi(not_scaped_article_doi, self.ingestion_number)
                # Delete article
                response = self.delete_article(article_doi)
                self.verify_http_code_is(response, NOT_FOUND)
                # Delete CRepo collections
                self.delete_test_collections(article_doi, bucket_name)
                # Delete CRepo objects
                self.delete_test_objects(article_doi, bucket_name)
        except AssertionError:
            logging.error('HTTP response code assertion error during article db deletion')
        return status_code

    def delete_test_collections(self, article_doi, bucket_name):
        """
        Get collection information from content repo using bucket name and article doi, then
        proceeded to call content repo delete collection endpoint
        :param article_doi: String. Such as '10.1371++journal.pone.0170224'
        :param bucket_name: String. Such as 'preprint'
        :return: None
        """
        self.get_collection_versions(bucketName=bucket_name, key=article_doi)
        collections = self.parsed.get_list()
        if collections:
            for coll in collections:
                response = self.delete_collection(bucketName=bucket_name, key=coll['key'],
                                       version=coll['versionNumber'])
                self.verify_http_code_is(response, OK)

    def delete_test_objects(self, article_doi, bucket_name):
        """
        Get object information from content repo using bucket name, then
        proceeded to call content repo delete object endpoint
        :param article_doi: String. Such as '10.1371++journal.pone.0170224'
        :param bucket_name: String. Such as 'preprint'
        :return: None
        """
        response = self.get_crepo_objets(bucketName=bucket_name)
        self.verify_http_code_is(response, OK)
        objects = self.parsed.get_list()
        if objects:
            test_objects = [item for item in objects if article_doi in item['key']]
            if test_objects:
                for object in test_objects:
                    response = self.delete_object(bucketName=bucket_name, key=object['key'],
                                       version=object['versionNumber'], purge=True)
                    self.verify_http_code_is(response, OK)

