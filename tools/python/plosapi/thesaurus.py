#!/usr/bin/env python
"""
PLOS Thesaurus API Module.

This module exports one class 'Thesaurus'

"""
from __future__ import print_function
from __future__ import with_statement

import os, sys, traceback, json, re, requests, md5
import StringIO
from lxml import etree
from xml.sax.saxutils import escape

__author__    = 'Bill OConnor'
__copyright__ = 'Copyright 2013, PLOS'
__version__   = '0.1'

class Thesaurus():
    """
    """
    _SERVER_URL = 'http://tax.plos.org:9080/servlet/dh'
    _REQUEST_TMPLT =  "<TMMAI project='plosthes.2013-6' location = '.'>" \
                         "<Method name='getSuggestedTermsFullPaths' returnType='java.util.Vector'/>"\
                         "<VectorParam>"\
                            "<VectorElement>{theText}</VectorElement>"\
                         "</VectorParam>"\
                      "</TMMAI>"
    _TERM_PATH = '/TMMAI/VectorReturn/VectorElement'

    def buildRequest(self, theText):
        """
        """
        return self._REQUEST_TMPLT.format(theText=escape(theText.encode('utf-8')))

    def getSubjectTerms(self, theText):
        """
        """
        reqStr = self.buildRequest(theText)
        r = requests.post(self._SERVER_URL, data=reqStr, headers={'Content-Type':'*/*'})
        if r.status_code == 200:
            terms = etree.fromstring(r.content).xpath(self._TERM_PATH)
            termList = [ re.split("^(<TERM>)(.*)(\|.*)(</TERM>)$", \
                         terms[t].text)[2] for t in range(1, len(terms)-1)]
            return ('OK', termList)
        else:
            return ('FAILED', [])

if __name__ == "__main__":
    """
    Main entry point for command line execution. 
    """
    import argparse
    import pprint

    parser = argparse.ArgumentParser(description='Thesaurus Client client module.')
    parser.add_argument('--file', help='Name of file containing text to assign subject areas.')
    parser.add_argument('command', help='subjects')
    parser.add_argument('params', nargs='*', help="command parameters")
    args = parser.parse_args()
    
    if args.file:
        fl = [open(args.file, 'rb')]
    else:
        fl = [ StringIO.StringIO(s) for s in args.params ]

    print('Start')

    if args.command == 'subjects':
        t = Thesaurus()
        for f in fl:
            (status, terms) = t.getSubjectTerms(f.read())
            if status == 'OK':
                for t in terms:
                    print(t)
        
