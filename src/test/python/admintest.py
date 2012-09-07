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

from restclient import Request
from sys import argv


_USAGE = """
Usage: admintest.py article_doi article_path asset_doi asset_path

    article_doi
        The DOI of the article to create, read, and delete.  The script
        will direct RESTful actions to a URL that refers to this article.

    article_path
        The local path to the XML file (NLM format) of the article to
        create.

    asset_doi
        The DOI of an asset to create and attach to the same article. It
        should be referred to within the article XML. Use the full DOI, not
        just the extension to the article DOI.

    asset_path
        The local path to a file that will be uploaded as the asset
        content.

An example is in runadmintest.sh.
"""


def report(description, rest_response):
    """Prepare an HTTP response for display as a string."""
    code, message = rest_response
    buf = [description, 'HTTP Status {0}'.format(code)]
    if message is None:
        buf.append('No response body')
    elif len(message) >= 80:
        buf += ['Response size: {0}'.format(len(message)),
                'Response head: {0!r}'.format(message[ :  40]),
                'Response tail: {0!r}'.format(message[-40 : ])]
    else:
        buf += ['Response body:', repr(message)]
    buf.append('')  # Extra blank line
    return '\n'.join(buf)

def test(article_doi, article_path, asset_doi, asset_path):
    """Run three test operations."""
    print 'Arguments:\n{0!r}\n'.format((article_doi, article_path, asset_doi, asset_path))

    def new_req(doi):
        return Request('localhost', 'article/' + doi, port=8080)

    create = new_req(article_doi)
    create.set_form_file_path('file', article_path)
    print report('Response to CREATE for article', create.post())

    create_asset = new_req(asset_doi)
    create_asset.set_form_file_path('file', asset_path)
    create_asset.set_query_parameter('assetOf', article_doi)
    print report('Response to CREATE for asset', create_asset.post())

    read = new_req(article_doi)
    print report('Response to READ', read.get())

    delete = new_req(article_doi)
    print report('Response to DELETE', delete.delete())


if len(argv) == 5:
    test(*argv[1:])
else:
    print _USAGE
