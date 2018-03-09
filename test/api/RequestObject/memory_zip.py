#!/usr/bin/env python2

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

"""
Class for creating an ingestible zip in memory.
"""

__author__ = 'rskonnord'

import sys

from test.Base.base_service_test import BaseServiceTest
from test.Base.MemoryZip import MemoryZipFile, MemoryZipData, build_zip_file_in_memory


class MemoryZipJSON(BaseServiceTest):

  def create_ingestible(self, article_doi, sub_dir):
    """
    Creates ingestible zip in memory by giving article doi and sub directory to find 
    mock data
    :param article_doi: String. Such as 'pone.0155391' 
    :param sub_dir: String. Such as "RelatedArticle/" to find mock data
    :return: none
    """
    if sys.version_info[0]>=3:
      EMPTY_PNG_FILE = bytes(
        '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x01'
        '\x03\x00\x00\x00%\xdbV\xca\x00\x00\x00\x03PLTE\x00\x00\x00\xa7z=\xda\x00'
        '\x00\x00\x01tRNS\x00@\xe6\xd8f\x00\x00\x00\nIDAT\x08\xd7c`\x00\x00\x00'
        '\x02\x00\x01\xe2!\xbc3\x00\x00\x00\x00IEND\xaeB`\x82', 'utf8')
    else:
      EMPTY_PNG_FILE = bytes(
        '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x01'
        '\x03\x00\x00\x00%\xdbV\xca\x00\x00\x00\x03PLTE\x00\x00\x00\xa7z=\xda\x00'
        '\x00\x00\x01tRNS\x00@\xe6\xd8f\x00\x00\x00\nIDAT\x08\xd7c`\x00\x00\x00'
        '\x02\x00\x01\xe2!\xbc3\x00\x00\x00\x00IEND\xaeB`\x82')

    REAL_FILES = [
      "manifest.dtd",
      "manifest.xml",
      article_doi + ".xml",
    ]

    if (sub_dir == 'RelatedArticle/'):
      DUMMY_FILES = [
      article_doi + ".g001.PNG_I",
      article_doi + ".g001.PNG_L",
      article_doi + ".g001.PNG_M",
      article_doi + ".g001.PNG_S",
      article_doi + ".g001.tif",
      article_doi + ".g002.PNG_I",
      article_doi + ".g002.PNG_L",
      article_doi + ".g002.PNG_M",
      article_doi + ".g002.PNG_S",
      article_doi + ".g002.tif",
      article_doi + ".g003.PNG_I",
      article_doi + ".g003.PNG_L",
      article_doi + ".g003.PNG_M",
      article_doi + ".g003.PNG_S",
      article_doi + ".g003.tif",
      article_doi + ".g004.PNG_I",
      article_doi + ".g004.PNG_L",
      article_doi + ".g004.PNG_M",
      article_doi + ".g004.PNG_S",
      article_doi + ".g004.tif",
      article_doi + ".pdf",
      article_doi + ".strk.PNG_I",
      article_doi + ".strk.PNG_L",
      article_doi + ".strk.PNG_M",
      article_doi + ".strk.PNG_S",
      article_doi + ".strk.tif",
      article_doi + ".xml.orig",
    ]
    else:
      DUMMY_FILES = [
      article_doi + ".pdf",
      article_doi + ".xml.orig",
      article_doi + ".s001.xlsx",
      article_doi + ".s002.tif",
      article_doi + ".s003.xlsx",
      article_doi + ".s004.tif",
      article_doi + ".s005.xlsx",
      article_doi + ".s006.xlsx",
      article_doi + ".s007.xlsx",
      article_doi + ".s008.xlsx",
      article_doi + ".s009.xlsx",
      article_doi + ".s010.xlsx",
      article_doi + ".s011.xlsx",
      article_doi + ".s012.jpg",
      article_doi + ".s013.xlsx",
      article_doi + ".s014.jpg",
      article_doi + ".s015.xlsx",
      article_doi + ".s016.tif",
      article_doi + ".s017.xlsx",
      article_doi + ".s018.tif",
      article_doi + ".s019.xlsx",
      article_doi + ".s020.tif",
      article_doi + ".s021.xlsx",
      article_doi + ".s022.tif",
      article_doi + ".s023.xlsx",
      article_doi + ".s024.tif",
      article_doi + ".s025.xlsx",
      article_doi + ".s026.tif",
    ]

    """
    Example usage of memory_zip module.
    """
    # Build MemoryZipFile objects from files containing actual test data.
    # This way we ONLY have to check in manifests and manuscripts.
    real_entries = [MemoryZipFile(f, 'test/data/'+ sub_dir + f) for f in REAL_FILES]

    # For large binary blobs whose contents don't matter to the test, we don't
    # want to check the files into Git. Instead, use MemoryZipData to mock them
    # out with placeholder bytes.
    data_entries = [MemoryZipData(f, EMPTY_PNG_FILE) for f in DUMMY_FILES]

    # Create an in-memory object that behaves like a file on disk
    zip_in_mem = build_zip_file_in_memory(real_entries + data_entries)

    return zip_in_mem
