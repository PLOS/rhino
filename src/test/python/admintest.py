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

def test(example_doi, path_to_sample_file):
    print (example_doi, path_to_sample_file)

    def new_req():
        return Request('localhost', 'article/' + example_doi, port=8080)

    create = new_req()
    create.set_form_file_path('file', path_to_sample_file)
    print 'Create result:', create.post()

    read = new_req()
    print 'Read result:  ', read.get()

    delete = new_req()
    print 'Delete result:', delete.delete()

test(*argv[1:])
