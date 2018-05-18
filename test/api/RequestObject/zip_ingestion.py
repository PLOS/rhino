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
Base class for Rhino ZIP Ingestion JSON related services
"""

from .ingestion import Ingestion
from ..resources import ZIP_INGESTION_API, ARTICLE_API, COLLECTIONS_API, OBJECTS_API, \
    DEFAULT_HEADERS, OK

__author__ = 'rskonnord'


class ZIPIngestionJson(Ingestion):

    def post_ingestible_zip(self, archive, bucket_name=None):
        """
        Calls article API to ingest a zip article file
        POST /zips
        :param archive: zip. Ingestible zip package containing article XML,manifest XML and images
        :param bucket_name: String. Optional paramenter
        :return: response
        """
        da_data = {'bucket': bucket_name}
        response = self.doPost(ZIP_INGESTION_API, {'archive': archive}, da_data)

        self.parse_response_as_json(response)

        return response

    # Article API
    def get_article(self, article_doi=None):
        """
        Calls article API to get an article
        GET /articles/{article_doi}...
        :param article_doi
        :return: response
        """
        response = self.doGet('{0}/{1}/{2}'.format(ARTICLE_API, article_doi, 'ingestions/1'), None,
                              headers=DEFAULT_HEADERS)
        self.parse_response_as_json(response)
        return response

    def delete_article(self, article_doi=None):
        """
        Calls article API to delete an article
        DELETE /articles/{article_doi}/revisions/1
        :param article_doi
        :return: response
        """
        response = self.doDelete('{0}/{1}/{2}'.format(ARTICLE_API, article_doi, 'revisions/1'),
                                 None, headers=DEFAULT_HEADERS)

        return response

    # Content Repo API
    def get_collection_versions(self, bucket_name=None, **kwargs):
        """
        Calls CREPO API to get a collection versions
        :param bucket_name: bucket name, key
        :return: response
        """
        response = self.doGet('{0}/versions/{1}'.format(COLLECTIONS_API, bucket_name),
                              params=kwargs, headers=DEFAULT_HEADERS)
        self.parse_response_as_json(response)
        return response

    def delete_collection(self, bucket_name=None, **kwargs):
        """
        Calls CREPO API to delete a collection
        :param bucket_name: bucket name.
        :return: response
        """
        response = self.doDelete('{0}/{1}'.format(COLLECTIONS_API, bucket_name), params=kwargs,
                                 headers=DEFAULT_HEADERS)
        if response.status_code != OK:
            self.parse_response_as_json(response)

        return response

    def get_crepo_objets(self, bucket_name=None, **kwargs):
        """
        Calls CREPO API to get objects list in a bucket
        GET /objects?bucket_name={bucket_name}...
        :param bucket_name, offset, limit, includeDeleted, includePurged, tag
        :return: response
        """
        response = self.doGet('{0}?bucket_name={1}'.format(OBJECTS_API, bucket_name), kwargs,
                              DEFAULT_HEADERS)
        self.parse_response_as_json(response)
        return response

    def delete_object(self, bucket_name=None, **kwargs):
        """
        Calls CREPO API to delete a object
        :param bucket_name: bucket name.
        :return: response
        """
        response = self.doDelete('{0}/{1}'.format(OBJECTS_API, bucket_name), params=kwargs,
                                 headers=DEFAULT_HEADERS)
        return response
