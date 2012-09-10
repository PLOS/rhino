#!/usr/bin/python2

# Copyright (c) 2012 by Public Library of Science
# http://plos.org
# http://ambraproject.org
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


"""Exercises the ArticleCrudController methods in Ambra Admin TNG."""

import httplib
import re
from restclient import Request
from sys import argv


_USAGE = """
Usage: admintest.py article_doi article_path asset_doi asset_path

    article_id
        The REST identifier of the article to create, read, and delete. The
        script will use a URL that refers to this article. For valid input,
        this should be the article's DOI with ".xml" appended.

    article_path
        The local path to the XML file (NLM format) of the article to
        create.

    asset_id
        The REST identifier of an asset to create and attach to the same
        article. For valid input, this should match the article DOI
        (conforming to Ambra's DOI-like convention for naming assets) and
        have an extension matching the file type.

    asset_path
        The local path to a file that will be uploaded as the asset
        content.

An example is in runadmintest.sh.
"""


SNIPPET_SIZE = 40

def code_message(code):
    """Translate an HTTP response code to its standard message."""
    try:
        return httplib.responses[code]
    except KeyError:
        return '(Undefined)'

def report(description, rest_response):
    """Prepare an HTTP response for display as a string."""
    code, message = rest_response
    buf = [description,
           'HTTP Status {0}: {1}'.format(code, code_message(code))]
    if message is None:
        buf.append('No response body')
    elif len(message) >= 80:
        buf += ['Response size: {0}'.format(len(message)),
                'Response head: {0!r}'.format(message[ :  SNIPPET_SIZE]),
                'Response tail: {0!r}'.format(message[-SNIPPET_SIZE : ])]
    else:
        buf += ['Response body:', repr(message)]
    buf.append('')  # Extra blank line
    return '\n'.join(buf)

def test(article_id, article_path, asset_id, asset_path):
    """Run test operations."""
    print 'Arguments:'
    for shell_arg in (article_id, article_path, asset_id, asset_path):
        print repr(shell_arg)
    print

    article_doi = re.match(r'(.*)\.xml', article_id).groups()[0]

    def new_req(doi):
        return Request('localhost', 'article/' + doi, port=8080)

    create = new_req(article_id)
    create.set_form_file_path('file', article_path)
    print report('Response to CREATE for article', create.post())

    create_asset = new_req(asset_id)
    create_asset.set_form_file_path('file', asset_path)
    create_asset.set_query_parameter('assetOf', article_doi)
    print report('Response to CREATE for asset', create_asset.post())

    read = new_req(article_id)
    print report('Response to READ', read.get())

    read_asset = new_req(asset_id)
    read_asset.set_form_file_path('file', asset_path)
    read_asset.set_query_parameter('assetOf', article_doi)
    print report('Response to READ for asset', read_asset.get())

    delete_asset = new_req(asset_id)
    delete_asset.set_form_file_path('file', asset_path)
    delete_asset.set_query_parameter('assetOf', article_doi)
    print report('Response to DELETE for asset', delete_asset.delete())

    delete = new_req(article_id)
    print report('Response to DELETE', delete.delete())


if len(argv) == 5:
    test(*argv[1:])
else:
    print _USAGE
