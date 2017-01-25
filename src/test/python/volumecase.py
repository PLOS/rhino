#!/usr/bin/python2

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
