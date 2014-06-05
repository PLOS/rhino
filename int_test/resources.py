#!/usr/bin/env python2
'''
This Resource File sets variables that are used in individual
test cases. It eventually should be replaced with more robust, 
less static, variable definitions. 
'''

friendly_testhostname = 'dpro'
base_url = 'http://one-' + friendly_testhostname + '.plosjournals.org'
authid = '00051c0e-a5dd-6d56-ac5c-3af158303189'
article_states = [ 'published', 'ingested', 'disabled' ]
art_synd_states = [ 'pending', 'in_progress', 'success', 'failure' ]
article_query =   '\"10\.1371/journal\.p[\w]{3}\.[\d]{7}\":\{\"doi\":\"info:doi/10\.1371/journal\.p[\w]{3}\.[\d]{7}\"\},?.*'
published_query = '\"10\.1371/journal\.p[\w]{3}\.[\d]{7}\":\{\"doi\":\"info:doi/10\.1371/journal\.p[\w]{3}\.[\d]{7}\",\"state\":\"\w+\"\},?.*'
ingested_query =  '\"10\.1371/journal\.p[\w]{3}\.[\d]{7}\":\{\"doi\":\"info:doi/10\.1371/journal\.p[\w]{3}\.[\d]{7}\",\"state\":\"\w+\"\},?.*'
#"10.1371/journal.pone.0069741":{"doi":"info:doi/10.1371/journal.pone.0069741","state":"disabled"}
disabled_query =  '.*'
pending_query = '(\"(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7}\":\{\"doi\":\"info:doi/(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7},?){*}'
in_progress_query = '(\"(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7}\":\{\"doi\":\"info:doi/(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7},?){*}'
success_query = '(\"(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7}\":\{\"doi\":\"info:doi/(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7},?){*}'
failure_query = '(\"(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7}\":\{\"doi\":\"info:doi/(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7},?){*}'
pingbacks_query = '(\"(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7}\":\{\"doi\":\"info:doi/(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7},\"title\":\".*?\",\"url\":\"http://dx.doi.org/(\d){2}\.(\d){4}%2Fjournal.p(\w){3}.(\d){7}\",\"pingbackCount\":(\d){*},\"mostRecentPingback\":\"(\d){4}-(\d){2}-(\d){2}T(\d){2}:(\d){2}:(\d){2}Z\"\},?){*}'
date_query = '(\"(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7}\":\{\"doi\":\"info:doi/(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7}\",\"lastModified\":\"(\d){4}-(\d){2}-(\d){2}T(\d){2}:(\d){2}:(\d){2}Z\"\},?){*}'
ingestibles_query = '(\"(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7}\":\{\"doi\":\"info:doi/(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7},?){*}'
journals_query = '(\"(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7}\":\{\"doi\":\"info:doi/(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7},?){*}'
config_query = '(\"(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7}\":\{\"doi\":\"info:doi/(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7},?){*}'
users_query = '(\"(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7}\":\{\"doi\":\"info:doi/(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7},?){*}'
users_authid_query='(\"(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7}\":\{\"doi\":\"info:doi/(\d){2}\.(\d){4}/journal\.p(\w){3}.(\d){7},?){*}'

