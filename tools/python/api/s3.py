#!/usr/bin/env python
"""
"""
from __future__ import print_function
from __future__ import with_statement
from cStringIO import StringIO
from boto.s3.connection import S3Connection
from boto.s3.connection import Location

import json
import os
import re
import requests
import string
import sys
import md5

class s3:
    """
    """
    _AWS_SECRET_ACCESS_KEY = os.environ['AWS_SECRET_ACCESS_KEY']
    _AWS_ACCESS_KEY_ID= os.environ['AWS_ACCESS_KEY_ID']
    _JRNL_IDS = ['pone', 'pmed', 'ppat', 'pbio', 'pgen', 'pcbi', 'pntd' ]

    def __init__(self, bucketID='us-west-1.pub.plos.org', prefix='10.1371'):
        """
        The S3 class is tied to a particular bucket and
        DOI prefix when it is initialized. This simplies
        matters when working with complete DOI and AFID's.
        """
        self.bucketID = bucketID
        self.prefix = prefix
        self.conn = S3Connection(aws_access_key_id=self._AWS_ACCESS_KEY_ID, 
                                     aws_secret_access_key=self._AWS_SECRET_ACCESS_KEY)
        self.bucket = self.conn.get_bucket(bucketID) 
        return

    def _ext2upper(self, fname):
        """
        """
        newfname = fname.split('.')
        newfname[-1] = newfname[-1].upper()
        return '.'.join(newfname)

    def _ext2lower(self, fname):
        """
        """
        newfname = fname.split('.')
        newfname[-1] = newfname[-1].lower()
        return '.'.join(newfname)

    def _doi2s3keyPath(self, doiSuffix):
        """
        Given a PLOS specific DOI return a PLOS specific S3 key
        path.

        PLOS DOI's follow the following formats:
 
        ex. 10.1371/journal.PLOSID.XXXXXXXX      (article DOI)
            10.1371/journal.PLOSID.XXXXXXX.gXXXX (asset DOI)
            10.1371/image.PLOSID.vXX.iXX         (image article DOI)
        
        If the key is structured properly it is easy to
        access S3 as if there is a hierarchial directory
        structure. Basically we want to replace the periods
        with '/' except in the case or the DOI prefix 10.1371.
        The prefix should remain intact.

        """
        keySuffix = string.replace(doiSuffix, '.', '/')
        return u'{p}/{s}/'.format(p=self.prefix, s=keySuffix)

    def _afid2s3key(self, afidSuffix):
        """
        Given a PLOS specific asset file ID (AFID) return a 
        PLOS specific S3 key.

        The AFID is fundementally a DOI with an extension 
        indicating the type/represention of the data contained
        within the asset. A completed S3 key will extract the DOI 
        portion of the AFID, convert it to an S3 key path, and
        append the afidSuffix.

        ex. 10.1371/journal.PLOSID.XXXXXXXX.XML -> 
            10.1371/journal/PLOSID/XXXXXXXX/PLOSID.XXXXXXXX.xml
            10.1371/image.PLOSID.vxx.ixx.xml ->
            10.1371/image/PLOSID/vxx/ixx/image.PLOSID.vxx.ixx.xml  
        """
        if afidSuffix.lower().startswith('journal'):
            doiSuffix = '/'.join(afidSuffix.lower().split('.')[:-1])
            newAFID = '.'.join(afidSuffix.lower().split('.')[1:])
            fullAFID = u'{p}/{s}/{a}'.format(p=self.prefix, s=doiSuffix, a=newAFID)
        elif afidSuffix.lower().startswith('image'):
            # Get everything except the extensions
            doiSuffix = '/'.join(afidSuffix.lower().split('.')[:-1])
            fullAFID = u'{p}/{s}/{a}'.format(p=self.prefix, s=doiSuffix, a=afidSuffix.lower())
        else:
            raise Exception('s3:invalid afid suffix ' + doiSuffix)
        return fullAFID

    def _s3keyPath2doi(self, s3keyPath):
        """
        """
        elemLst = s3keyPath.split('/')
        return u'{p}/{s}'.format(p=elemLst[0], s= '.'.join(elemLst[1:-1]))

    def _s3key2afid(self, s3key):
        """
        """
        elemLst = s3key.split('/')
        elemLst[-1] = self._ext2upper(elemLst[-1])
        if elemLst[1].lower() == 'journal':
            fullAFID = u'{p}/journal.{s}'.format(p=elemLst[0], s= '.'.join(elemLst[-1:]))    
        elif  elemLst[1].lower() == 'image':
            fullAFID = u'{p}/{s}'.format(p=elemLst[0], s= '.'.join(elemLst[-1:]))
        else:
            raise Exception('s3:invalid s3 key ' + s3key)
        return fullAFID

    def _getAssetMeta(self, afidSuffix):
        """
        """
        fullKey = self._afid2s3key(afidSuffix)
        mdata = self.bucket.get_key(fullKey).metadata
        result = dict()
        for k in mdata.iterkeys():
            lk = k.lower()
            if lk == 'asset-contenttype':
                result['contentType'] = mdata[k]
            elif lk == 'asset-contextelement':
                result['contextElement'] = mdata[k]
            elif lk == 'asset-created':
                result['created'] = mdata[k]
            elif lk == 'asset-doi':
                result['doi'] = mdata[k]
            elif lk == 'asset-extension':
                result['extension'] = mdata[k]
            elif lk == 'asset-lastmodified':
                result['lastModified'] = mdata[k]
            elif lk == 'asset-title':
                result['lastModified'] = mdata[k]
            elif lk == 'asset-size':
                result['size'] = mdata[k]
        result['md5'] = mdata['asset-md5']
        result['description'] = 'S3 does not support descriptions'
        return result
         
    def _getBinary(self, fname):
        return

    def buckets(self):
        """
        """
        return self.conn.get_all_buckets()

    def keycheck(self, afidSuffix):
        """
        """
        fullKey = self._afid2s3key(afidSuffix)
        return self.bucket.get_key(fullKey)

    def bucket_names(self):
        """
        """
        return [ b.name for b in self.conn.get_all_buckets()]

    def articles(self):
        """
        """
        # Get the image article DOIs
        bklstRslt = self.bucket.list(delimiter='/', prefix=self.prefix + '/image/')
        for p1 in bklstRslt:
            bklstRslt2 = self.bucket.list(delimiter='/', prefix=p1.name)
            for p2 in bklstRslt2:
                bklstRslt3 = self.bucket.list(delimiter='/', prefix=p2.name)
                for k in bklstRslt3:
                   yield self._s3keyPath2doi(k.name)
        # Get the journal DOIs 
        prefixLst = [ '{p}/journal/{id}/'.format(p=self.prefix, id=jrnlid) for jrnlid in self._JRNL_IDS ] 
        for p in prefixLst:
            bklstRslt = self.bucket.list(delimiter='/', prefix=p) 
            for k in bklstRslt:
                yield self._s3keyPath2doi(k.name)
    
    def article(self, doiSuffix):
        """
        """
        raise Exception('s3:article not supported')    

    def assets(self, doiSuffix):
        """
        """
        artDOI = '{p}/{s}'.format(p=self.prefix, s=doiSuffix)
        assets = {}
        afids = [] 
        assetDOIs = []
        s3keyPath = self._doi2s3keyPath(doiSuffix)

        # Pass 1: breakout xml and pdf afids from asset DOIs
        bklstRslt = self.bucket.list(delimiter='/', prefix=s3keyPath)
        for k in bklstRslt:
            if k.name.endswith('/'):
                assetDOIs.append(k.name)
            else:
                afids.append(self._s3key2afid(k.name))
        assets[artDOI] = afids
        
        # Pass 2: process the asset DOIs
        for assetDOI in assetDOIs:
            afids = []
            bklstRslt = self.bucket.list(delimiter='/', prefix=assetDOI)
            for k in bklstRslt:
                afids.append(self._s3key2afid(k.name))
            assets[self._s3keyPath2doi(assetDOI)] = afids
        return assets

    def asset(self, doiSuffix):
        """
        """
        pp = pprint.PrettyPrinter(indent=2)
        theAssets = self.assets(doiSuffix)
        assetRslt = dict()
        for (doi, afids) in theAssets.iteritems():
            for fullAFID in afids:
                afid = fullAFID.split('/')
                if afid[0] == self.prefix:
                    assetRslt[afid[1]] = self._getAssetMeta(afid[1])
                else:
                    raise Exception('s3:invalid s3 prefix ' + fullAFID)
        return assetRslt

    def assetFile(self, afidSuffix, fname=None):
        """
        Retreive the actual asset data. If the name is not
        specified use the afid as the file name.
        """
        if fname == None:
            fname = self._ext2upper(afidSuffix)
        fullKey = self._afid2s3key(afidSuffix)
        return self.bucket.get_key(fullKey).get_contents_to_filename(fname)
        
if __name__ == "__main__":
    """
    """
    import argparse
    import pprint
    pp = pprint.PrettyPrinter(indent=2)
    parser = argparse.ArgumentParser(description='S3 API client module.')
    parser.add_argument('command', help="articles, article")
    parser.add_argument('doiList', nargs='*', help="list of doi's")
    args = parser.parse_args()

    if args.command == 'buckets':
        s3 = s3()
        names = s3.bucket_names()
        for n in names:
            print(n)
    elif args.command == 'keycheck':
        s3 = s3()
        for afid in args.doiList:
           theKey = s3.keycheck(afid)
           pp.pprint(theKey.metadata)
    elif args.command == 'articles':
        s3 = s3()
        for k in s3.articles():
            print(k)
    elif args.command == 'assets':
        s3 = s3()
        for doi in args.doiList:
           pp.pprint(s3.assets(doi))
    elif args.command == 'asset':
        s3 = s3()
        for doi in args.doiList:
           pp.pprint(s3.asset(doi))
    elif args.command == 'assetfile':
        s3 = s3()
        for afid in args.doiList:
            s3.assetFile(afid)
