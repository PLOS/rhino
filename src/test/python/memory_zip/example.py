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

__author__ = 'rskonnord'

import requests

from memory_zip import *

EMPTY_PNG_FILE = bytes(
    '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x01'
    '\x03\x00\x00\x00%\xdbV\xca\x00\x00\x00\x03PLTE\x00\x00\x00\xa7z=\xda\x00'
    '\x00\x00\x01tRNS\x00@\xe6\xd8f\x00\x00\x00\nIDAT\x08\xd7c`\x00\x00\x00'
    '\x02\x00\x01\xe2!\xbc3\x00\x00\x00\x00IEND\xaeB`\x82')

REAL_FILES = [
    "manifest.dtd",
    "manifest.xml",
    "pone.0170224.xml",
]

DUMMY_FILES = [
    "pone.0170224.g001.PNG_I",
    "pone.0170224.g001.PNG_L",
    "pone.0170224.g001.PNG_M",
    "pone.0170224.g001.PNG_S",
    "pone.0170224.g001.tif",
    "pone.0170224.g002.PNG_I",
    "pone.0170224.g002.PNG_L",
    "pone.0170224.g002.PNG_M",
    "pone.0170224.g002.PNG_S",
    "pone.0170224.g002.tif",
    "pone.0170224.g003.PNG_I",
    "pone.0170224.g003.PNG_L",
    "pone.0170224.g003.PNG_M",
    "pone.0170224.g003.PNG_S",
    "pone.0170224.g003.tif",
    "pone.0170224.g004.PNG_I",
    "pone.0170224.g004.PNG_L",
    "pone.0170224.g004.PNG_M",
    "pone.0170224.g004.PNG_S",
    "pone.0170224.g004.tif",
    "pone.0170224.pdf",
    "pone.0170224.strk.PNG_I",
    "pone.0170224.strk.PNG_L",
    "pone.0170224.strk.PNG_M",
    "pone.0170224.strk.PNG_S",
    "pone.0170224.strk.tif",
    "pone.0170224.xml.orig",
]

def example():
    """Example usage of memory_zip module."""

    # Build MemoryZipFile objects from files containing actual test data.
    # This way we ONLY have to check in manifests and manuscripts.
    real_entries = [MemoryZipFile(f, 'testdata/' + f) for f in REAL_FILES]

    # For large binary blobs whose contents don't matter to the test, we don't
    # want to check the files into Git. Instead, use MemoryZipData to mock them
    # out with placeholder bytes.
    data_entries = [MemoryZipData(f, EMPTY_PNG_FILE) for f in DUMMY_FILES]

    # Create an in-memory object that behaves like a file on disk
    zip_in_mem = build_zip_file_in_memory(real_entries + data_entries)

    response = requests.post('http://localhost:8081/articles',
                             files={'archive': zip_in_mem})
    print(response.status_code)

example()
