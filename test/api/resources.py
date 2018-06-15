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
This Resource File sets variables that are used in individual
test cases.
"""

from test.Base.Config import API_BASE_URL
from test.Base.Config import repo_config

# General resources

# Default headers
DEFAULT_HEADERS = {'Accept': 'application/json'}

# Get BUCKET_NAME
RELATED_ARTICLE_BUCKET_NAME = 'mogilefs-prod-repo'
PREPRINT_ARTICLE_BUCKET_NAME = 'preprints'

# URL's API
ZIP_INGESTION_API = API_BASE_URL + '/articles'
ARTICLE_API = API_BASE_URL + '/articles'

CREPO_BASE_URL = str(repo_config['transport']) + '://' + str(repo_config['host']) + ':' + \
                 str(repo_config['port']) + str(repo_config['path'])

OBJECTS_API = CREPO_BASE_URL + '/objects'
COLLECTIONS_API = CREPO_BASE_URL + '/collections'

# Related article DOI
RELATED_ARTICLE_DOI = '10.1371++journal.pone.0170224'
NOT_SCAPE_RELATED_ARTICLE_DOI = '10.1371/journal.pone.0170224'

# Preprint article DOI
PREPRINT_ARTICLE_DOI = '10.24196++journal.aarx.2001421'
NOT_SCAPE_PREPRINT_ARTICLE_DOI = '10.24196/journal.aarx.2001421'

# ZIP files
ZIP_ARTICLE = 'pone.0170224.zip'

# related article doi
RA_DOI = 'pone.0170224'

# preprint article doi
PP_DOI = 'aarx.2001421'

# Variables used to verify article revisions
REVISION = 1
INGESTION_NUMBER = 1

# Http Codes
OK = 200
CREATED = 201
BAD_REQUEST = 400
METHOD_NOT_ALLOWED = 405
NOT_FOUND = 404

# Article's states
ACTIVE = 0
UNPUBLISHED = 1
DISABLED = 2

# Article's status
INGESTED = 'ingested'

# Article File
PDF_CONTENT_TYPE = 'application/pdf'
XML_CONTENT_TYPE = 'text/xml'
