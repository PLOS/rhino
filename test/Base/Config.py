#!/usr/bin/env python2

import os

'''
  General resources
'''

# Set API_BASE_URL environment variable to desired URL in order to run suite against it
API_BASE_URL = os.getenv('API_BASE_URL', 'http://one-fluffy.plosjournals.org/api')

PRINT_DEBUG = False

TIMEOUT = 30         # API call timeout, in seconds

dbconfig = {
  'user': 'root',
  'password': '',
  'host': '127.0.0.1', # 'iad-leo-devstack01.int.plos.org', Can't access it from my box. No ICMP nor 3306 port
  'database': 'ambra',
  #'raise_on_warnings': True,
}
