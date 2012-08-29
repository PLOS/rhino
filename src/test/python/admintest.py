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
Usage: admintest.py doi path

    doi
        The script builds the URL to which the RESTful actions are directed
        by using this value as an article DOI.
    path
        The local path to the file whose contents will be submitted as the
        article XML (NLM format).

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

def test(example_doi, path_to_sample_file):
    """Run three test operations."""
    print 'Arguments:\n{0!r}\n'.format((example_doi, path_to_sample_file))

    def new_req():
        return Request('localhost', 'article/' + example_doi, port=8080)

    create = new_req()
    create.set_form_file_path('file', path_to_sample_file)
    #create.set_query_parameter('assetOf', 'asdf')
    print report('Response to CREATE', create.post())

    read = new_req()
    print report('Response to READ', read.get())

    delete = new_req()
    print report('Response to DELETE', delete.delete())


if len(argv) == 3:
    test(*argv[1:])
else:
    print _USAGE
