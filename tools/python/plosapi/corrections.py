#!/usr/bin/env python
"""
    Corrections harvester

    We needed the xml for all the annottaion corrections that
    were submitted to PMC. Using ftp://ftp.ncbi.nlm.nih.gov/pub/pmc/PMC-ids.csv.gz
    I the filtered out the all but the corrections into a csv.
    The csv was then used as input to collect the xml. 
"""
from __future__ import print_function
from __future__ import with_statement

import os, sys, traceback, json, re, requests, md5, csv

__author__    = 'Bill OConnor'
__copyright__ = 'Copyright 2013, PLOS'
__version__   = '0.1'

class Corrections:
    _OAI_XMLTMPL = 'http://www.pubmedcentral.nih.gov/oai/oai.cgi?verb=GetRecord&identifier=oai:pubmedcentral.nih.gov:{id}&metadataPrefix=pmc'
    
    def __init__(self):
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

    def _getBinary(self, fname, url):
        """
        Most of the files other than the article xml are binary in nature.
        Fetch the data and write it to a temporary file. Return the MD5
        hash of the fle contents.
        """
        m = md5.new()
        r = self._doGet(url)
        if r.status_code == 200:
            with open(fname, 'wb') as f:
                for chunk in r.iter_content(1024):
                    m.update(chunk)
                    f.write(chunk)
                f.close()
        else:
            raise Exception('rhino:failed to get binary ' + url)
        return m.hexdigest()

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
                doi = row[6].replace('/','_') + '.xml'
                # The PMCID in the 9th column
                pmcid = row[8].replace('PMC', '')
                url = self._OAI_XMLTMPL.format(id=pmcid)
                md5 = self._getBinary(doi, url)
                yield (pmcid, doi, md5)
            
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
        
    
