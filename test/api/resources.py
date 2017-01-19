#!/usr/bin/env python2
'''
This Resource File sets variables that are used in individual
test cases.
'''

from ..Base.Config import API_BASE_URL
from ..Base.Config import repo_config

# General resources

# Default headers
DEFAULT_HEADERS = {'Accept': 'application/json'}

#Get BUCKET_NAME based on whether API_BASE_URL is on prod or not.
BUCKET_NAME = u'mogilefs-prod-repo'
#if API_BASE_URL.split('/')[2] in ('sfo-perf-plosrepo01.int.plos.org:8002', 'rwc-prod-plosrepo.int.plos.org:8002'):
#  BUCKET_NAME = u'mogilefs-prod-repo'

#URL's API
ZIP_INGESTION_API = API_BASE_URL + '/articles'
ARTICLE_API = API_BASE_URL + '/articles'

CREPO_BASE_URL =  str(repo_config['transport']) + '://' + str(repo_config['host']) + ':' + \
                  str(repo_config['port']) + str(repo_config['path'])

OBJECTS_API = CREPO_BASE_URL + '/objects'
COLLECTIONS_API = CREPO_BASE_URL + '/collections'

#Article DOI
ARTICLE_DOI = '10.1371++journal.pone.0155391'

NOT_SCAPE_ARTICLE_DOI= '10.1371/journal.pone.0155391'

#ZIP files
ZIP_ARTICLE = 'pone.0155391.zip'

#Variables used to verify article revisions
REVISION = 1
INGESTION_NUMBER = 1

# Http Codes
OK = 200
CREATED = 201
BAD_REQUEST = 400
METHOD_NOT_ALLOWED = 405
NOT_FOUND = 404

#Article's states
ACTIVE = 0
UNPUBLISHED = 1
DISABLED = 2

#Article's status
INGESTED = 'ingested'

#Article File
PDF_CONTENT_TYPE = 'application/pdf'
XML_CONTENT_TYPE = 'text/xml'


