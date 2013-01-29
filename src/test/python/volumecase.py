#!/usr/bin/python2

# Copyright (c) 2012-2013 by Public Library of Science
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


"""Test case representations for volumes and issues."""


class VolumeCase(object):
    """One test case of a volume to create."""
    def __init__(self, doi, journal_key, display_name, issues=()):
        self.doi = DOI_PREFIX + doi
        self.journal_key = journal_key
        self.display_name = display_name
        self.issues = issues

    def __str__(self):
        return 'TestVolume({0!r}, {1!r}, {2!r}, {3!r})'.format(
            self.doi, self.journal_key, self.display_name, self.issues)

class IssueCase(object):
    """One test case of an issue to create.

    In order to be created, an instance should belong to the 'issues' field
    of a TestVolume object.
    """
    def __init__(self, suffix, display_name, image_uri=None):
        if not suffix.startswith('.'):
            suffix = '.' + suffix
        self.suffix = suffix
        self.display_name = display_name
        self.image_uri = image_uri

    def __str__(self):
        return 'TestIssue({0!r}, {1!r}, {2!r})'.format(
            self.suffix, self.display_name, self.image_uri)

TEST_VOLUMES = [
    VolumeCase('volume.pone.v47', 'PLoSONE', 'TestVolume',
               issues=[TestIssue('i23', 'TestIssue')]),
    ]
"""A list of cases to use."""
