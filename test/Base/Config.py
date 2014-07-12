#!/usr/bin/env python2

import os

'''
  General resources
'''

# Set RHINO_URL environment variable to desired URL in order to run suite against it
RHINO_URL = os.getenv('RHINO_URL', 'http://one-dpro2.plosjournals.org/api')

dbconfig = {
  'user': 'root',
  'password': '',
  'host': '127.0.0.1', # 'iad-leo-devstack01.int.plos.org', Can't access it from my box. No ICMP nor 3306 port
  'database': 'ambra',
  #'raise_on_warnings': True,
}