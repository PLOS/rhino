#!/usr/bin/python3
# -*- coding: utf-8 -*-

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

__author__ = 'rskonnord'

"""Utility for creating zip files in memory."""

import io
import zipfile

class MemoryZipEntry(object):
    """An entry to be inserted into an in-memory zip file."""

    def __init__(self, name):
        self.name = name

    def get_file(self):
        """
        Streams data by calling _get_file_bytes()
        :param None
        :return: Stream data return from _get_file_bytes()
        """
        try:
            output = io.BytesIO(self._get_file_bytes())
        except TypeError:
            output = io.BytesIO(self._get_file_bytes().encode(encoding='utf-8'))

        return output

class MemoryZipData(MemoryZipEntry):
    """An entry containing data that is already in memory."""

    def __init__(self, name, data):
        MemoryZipEntry.__init__(self, name)
        self._data = data

    def _get_file_bytes(self):
        """
        Returns data that is ready in memory
        :param None
        :return: Return data store in memory in _data
        """
        if isinstance(self._data, str):
            return bytearray(self._data)
        return self._data

class MemoryZipFile(MemoryZipEntry):
    """An entry pointing to a file on disk."""

    def __init__(self, name, filename):
        MemoryZipEntry.__init__(self, name)
        self._filename = filename

    def _get_file_bytes(self):
        """
        Entry point to a file in disk
        :param None
        :return: Return file read handler
        """
        with open(self._filename, mode='rb') as f:
            return f.read()

def build_zip_file_in_memory(entries):
    """
    Creates a zip file by writes files and data to a zip file
    :param entries is compose of MemoryZipFile objects and MemoryZipData 
    :return: File-like object containing a zip file, backed in memory.
    """
    zip_bin = io.BytesIO()
    with zipfile.ZipFile(zip_bin, 'w') as zip_file:
        for entry in entries:
            zip_file.writestr(entry.name, entry.get_file().read())

    zip_bin.seek(0)
    return zip_bin
