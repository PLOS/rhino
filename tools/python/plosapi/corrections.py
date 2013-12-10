#!/usr/bin/env python
"""
    Corrections harvester

    We needed the xml for all the annottaion corrections that
    were submitted to PMC. Using ftp://ftp.ncbi.nlm.nih.gov/pub/pmc/PMC-ids.csv.gz
    I the filtered out the all but the corrections into a csv.
    The csv was then used as input to collect the xml. 

    Note a couple of bogus things done:
    1. The DOI and PMCID are in specific columns of the CSV
    2. The bogus string replacements to get rid of namespace 
       stuff. (XML rant here)

    
"""
from __future__ import print_function
from __future__ import with_statement

import os, sys, traceback, json, re, requests, md5, csv
from lxml import etree

__author__    = 'Bill OConnor'
__copyright__ = 'Copyright 2013, PLOS'
__version__   = '0.1'

class Corrections:

    _HEADER = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE article
  PUBLIC "-//NLM//DTD Journal Publishing DTD v3.0 20080202//EN" "http://dtd.nlm.nih.gov/publishing/3.0/journalpublishing3.dtd">
"""

    _OAI_XML_TMPL = 'http://www.pubmedcentral.nih.gov/oai/oai.cgi?verb=GetRecord&identifier=oai:pubmedcentral.nih.gov:{id}&metadataPrefix=pmc'
    
    _NSPACE1 = '{http://www.openarchives.org/OAI/2.0/}'
    _NSPACE2 = '{http://dtd.nlm.nih.gov/2.0/xsd/archivearticle}'
    _QUERY_STR = './/{ns1}GetRecord/{ns1}record/{ns1}metadata'

    def __init__(self):
        self._QUERY_STR = self._QUERY_STR.format(ns1=self._NSPACE1, ns2=self._NSPACE2)
        return

    def _doGet(self, url):
        """
        Requests for Humans not so human after all.
        The verfiy parameter fails if the URL is not https:(
        """
        if url.lower().startswith('https:'):
            return requests.get(url, verify=self.verify)
        else:
            return requests.get(url)

    def _getBinary(self, url):
        """
        Most of the files other than the article xml are binary in nature.
        Fetch the data and write it to a temporary file. Return the MD5
        hash of the fle contents.
        """
        m = md5.new()
        r = self._doGet(url)
        if r.status_code == 200:
            m.update(r.content)
        else:
            raise Exception('rhino:failed to get binary ' + url)
        return (r.content, m.hexdigest())

    def _getMD5(self, url):
        """
        Read from the URL byte by byte calculating
        the MD5 hash. 
        """
        m = md5.new()
        r = self._doGet(url)
        if r.status_code == 200:
           for chunk in r.iter_content(1024):
               m.update(chunk)
        else:
           raise Exception('rhino:failed to get MD5 ' + url)
        return m.hexdigest()

    def harvest(self, csvFilename):
        """
        Using the PMCID's in the csv use PubMeds harvester to 
        down load the files. This was originally used to fetch
        annotation corrections but could be used for with
        any appropriately formatted csv. 
        """
        with open(csvFilename, 'rb') as csvIn:
            dirName = 'corrections_harvest_dir'
            os.mkdir(dirName)
            os.chdir('./'+ dirName)
            reader = csv.reader(csvIn, delimiter=',', quotechar='"')
            for row in reader:
                # The DOI is expected in the 7th column
                # Use it for the file name.
                fname = row[6].replace('/','_') + '.xml'
                # The PMCID in the 9th column
                pmcid = row[8].replace('PMC', '')
                url = self._OAI_XML_TMPL.format(id=pmcid)
                (text, md5) = self._getBinary(url)
                tree = etree.fromstring(text)
                root = tree.findall(self._QUERY_STR)[0]
                child = root[0]
                newText = etree.tostring(child,  encoding='utf-8')
                with open(fname, 'wb') as f:
                    f.write(self._HEADER)
                    f.write(newText)
                    f.close()
                yield (pmcid, fname, md5)
        os.chdir('../')

if __name__ == "__main__":
    import argparse   
    import pprint 
 
    # Main command dispatcher.
    dispatch = { 'harvest' : lambda fname, obj : [ id for id in obj.harvest(fname[0])  ],
               }

    pp = pprint.PrettyPrinter(indent=2)
    parser = argparse.ArgumentParser(description='Corrections Harvester')
    parser.add_argument('command', help="harvest")
    parser.add_argument('correctionsCSV', nargs='*', help="CSV data file of articles to be harvested")
    args = parser.parse_args()

    try:
        for val in dispatch[args.command](args.correctionsCSV, Corrections()):
            pp.pprint(val)
    except Exception as e:
        sys.stderr.write('Exception: {msg}.\n'.format(msg=e.message))
        traceback.print_exc(file=sys.stdout)
        sys.exit(1)

    sys.exit(0)
        
    
