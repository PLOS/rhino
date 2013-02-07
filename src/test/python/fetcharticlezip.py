#!/usr/bin/python2

# Copyright (c) 2013 by Public Library of Science
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

"""Scrape article XMLs and package them for ingestion."""

from __future__ import print_function
from cStringIO import StringIO
import os
import requests
import zipfile


DTD_TEXT = """
<!ELEMENT manifest (articleBundle) >
<!ELEMENT articleBundle (article, object*) >
<!ELEMENT article (representation+) >
<!ATTLIST article
    uri         CDATA          #REQUIRED
    main-entry  CDATA          #REQUIRED >
<!ELEMENT object (representation+) >
<!ATTLIST object
    uri         CDATA          #REQUIRED >
<!ELEMENT representation EMPTY >
<!ATTLIST representation
    name        CDATA          #REQUIRED
    entry       CDATA          #REQUIRED >
""".lstrip()

MANIFEST_TEMPLATE = """
<?xml version="1.1"?>
<!DOCTYPE manifest SYSTEM "manifest.dtd">
<manifest>
  <articleBundle>
    <article uri="info:doi/10.1371/journal.{doi}" main-entry="{doi}.xml">
      <representation name="XML" entry="{doi}.xml" />
    </article>
  </articleBundle>
</manifest>
""".lstrip()

FETCH_XML_URL_TEMPLATE = (
    'http://www.plosone.org/article/fetchObjectAttachment.action'
    '?uri=info%3Adoi%2F10.1371%2Fjournal.{doi}&representation=XML'
    )

OUTPUT_PATH = 'zips'

def make_zip(doi):
    manifest_xml = MANIFEST_TEMPLATE.format(doi=doi)

    fetch = requests.get(FETCH_XML_URL_TEMPLATE.format(doi=doi))
    xml_path = os.path.join(OUTPUT_PATH, doi + '.xml')
    with open(xml_path, mode='w') as xml_file:
        xml_file.write(fetch.content)

    compress = zipfile.ZIP_DEFLATED
    zip_path = os.path.join(OUTPUT_PATH, doi + '.zip')
    with zipfile.ZipFile(zip_path, mode='w') as zf:
        zf.writestr('manifest.dtd', DTD_TEXT, compress_type=compress)
        zf.writestr('MANIFEST.xml', manifest_xml, compress_type=compress)
        zf.writestr(doi + '.xml', fetch.content, compress_type=compress)


INTERESTING_ARTICLES = [
    'pbio.0030408',
    'pbio.0040088',
    'pbio.1001199',
    'pbio.1001315',
    'pgen.1002912',
    'pmed.0020007',
    'pmed.0020402',
    'pmed.0030132',
    'pmed.0030445',
    'pmed.1000431',
    'pmed.1001300',
    'pone.0000000',
    'pone.0005723',
    'pone.0008519',
    'pone.0016329',
    'pone.0016976',
    'pone.0026358',
    'pone.0028031',
    'ppat.0040045',
    'ppat.1000166',
    'ppat.1002247',
    'ppat.1002735',
    ]
"""From https://developer.plos.org/confluence/display/Ambra2/Interesting+articles"""

for doi in INTERESTING_ARTICLES:
    make_zip(doi)
make_zip('pone.0038869')
