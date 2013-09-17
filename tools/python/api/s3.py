#!/usr/bin/env python
"""
    PLOS S3 repo module.

    Some definitions:

        DOI: a unique identifier  

"""
from __future__ import print_function
from __future__ import with_statement
from cStringIO import StringIO
from boto.s3.connection import S3Connection
from boto.s3.connection import Location

import os, sys, re, string, requests, md5, json

__author__    = 'Bill OConnor'
__copyright__ = 'Copyright 2013, PLOS'
__version__   = '0.1'

class S3:
    """
    """
    _FOREWARD_MDATA_MAP = { 'asset-contenttype':'contentType',
                         'asset-contextelement' : 'contextElement',
                         'asset-created' : 'created',
                         'asset-doi': 'doi',
                         'asset-extension': 'extension',
                         'asset-lastmodified': 'lastModified',
                         'asset-title': 'lastModified',
                         'asset-size': 'size'
                       }
    _REVERSE_MDATA_MAP = { 'contentType' : 'asset-contenttype',
                           'contextElement' : 'asset-contextelement',
                           'created' : 'asset-created',
                           'doi' : 'asset-doi',
                           'extension' : 'asset-extension',
                           'lastModified' : 'asset-lastmodified',
                           'lastModified' : 'asset-title',
                           'size' : 'asset-size'
                         }

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

    def _forewardmap(self, name):
        """
        Map a s3 meta-data name to the rhino equivalent.
        """
        if self._FOREWARD_MDATA_MAP.has_key(name):
            return self._FOREWARD_MDATA_MAP[name]
        else:
            return None

    def _reversemap(self, name): 
        """
        Map a rhino meta-data name to the s3 equivalent.
        """
        if self._REVERSE_MDATA_MAP.has_key(name):
            return self._REVERSE_MDATA_MAP[name]
        else:
            return None

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
            raise Exception('s3:invalid afid suffix ' + afidSuffix)
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
        elif elemLst[1].lower() == 'image':
            fullAFID = u'{p}/{s}'.format(p=elemLst[0], s= '.'.join(elemLst[-1:]))
        else:
            raise Exception('s3:invalid s3 key ' + s3key)
        return fullAFID
    
    def _afidsFromDoi(self, doiSuffix):
        """
        Given a DOI Suffix return a list of AFIDs 
        """
        assets = self.assets(doiSuffix)
        for (adoi, afids) in assets.iteritems():
            for fullAFID in afids:
                (p, afid) = fullAFID.split('/')
                yield afid

    def _getAssetMeta(self, afidSuffix):
        """
        """
        fullKey = self._afid2s3key(afidSuffix)
        mdata = self.bucket.get_key(fullKey).metadata
        result = dict()
        for k in mdata.iterkeys():
            lk = k.lower()
            mappedKey = self._forewardmap(lk)
            if mappedKey:
                result[self._forewardmap(lk)] = mdata[k]
        
        result['md5'] = mdata['asset-md5']
        result['description'] = 'S3 does not support descriptions'
        return result
         
    def _getBinary(self, fname, fullKey):
        """
        Most of the files other than the article xml are binary in nature.
        Fetch the data and write it to a temporary file. Return the MD5
        hash of the fle contents.
        """
        m = md5.new()
        k = self.bucket.get_key(fullKey)
        with open(fname, 'wb') as f:
            for chunk in k:
                m.update(chunk)
                f.write(chunk)
            f.close()
        return m.hexdigest() 

    def buckets(self):
        """
        """
        return self.conn.get_all_buckets()

    def keycheck(self, afidSuffix):
        """
        """
        fullKey = self._afid2s3key(afidSuffix)
        # keys = self.bucket.list_versions(prefix=fullKey, delimiter='/')
        keys = [ self.bucket.get_key(fullKey) ]
        for k in keys:
            mdata = k.metadata
            mdata['S3:name'] = k.name
            mdata['S3:cache_control'] = k.cache_control
            mdata['S3:content_type'] = k.content_type
            mdata['S3:content_encoding'] = k.content_encoding
            mdata['S3:content_disposition'] = k.content_disposition
            mdata['S3:content_language'] = k.content_language
            mdata['S3:etag'] = k.etag
            mdata['S3:last_modified'] = k.last_modified
            mdata['S3:owner'] = k.owner
            mdata['S3:storage_class'] = k.storage_class
            mdata['S3:md5'] = k.md5
            mdata['S3:size'] = k.size
            mdata['S3:version_id'] = k.version_id
            mdata['S3:encrypted'] = k.encrypted        
        return mdata

    def bucket_names(self):
        """
        """
        return [ b.name for b in self.conn.get_all_buckets()]

    def articles(self):
        """
        Get a list of DOIs from the s3 bucket keys. This is some what
        DOI specific. For image articles we need to iterate over 3
        levels. For journals 2. 
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
        Since s3 in only storing article data at this point 
        the article meta-data is not available.
        """
        raise NotImplementedError('s3:article not supported')    

    def rmArticle(self, doiSuffix):
        """
        """
        for afid in self._afidsFromDoi(doiSuffix):
            print(self._afid2s3key(afid))

    def assets(self, doiSuffix):
        """
        Return a map with ADOI's as keys and a list of
        AFIDs for each ADOI.
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

    def asset(self, adoiSuffix):
        """
        Given an ADOI dump the meta-data 
        """
        assets = self.assets(adoiSuffix)
        result = dict()
        fullDOI = '{p}/{s}'.format(p=self.prefix, s=adoiSuffix)
        afids = assets[fullDOI]
        for fullAFID in afids:
           afid = fullAFID.split('/')
           if afid[0] == self.prefix:
               result[fullAFID] = self._getAssetMeta(afid[1])
           else:
               raise Exception('s3:invalid s3 prefix ' + fullAFID)
        return result

    def assetall(self, adoiSuffix):
        """
        
        """
        assets = self.assets(adoiSuffix)
        result = dict()
        for (adoi, afids) in assets.iteritems():
            for fullAFID in afids:
                afid = fullAFID.split('/')
                result[fullAFID] = self._getAssetMeta(afid[1])
        return result

    def assetFile(self, afidSuffix, fname=None):
        """
        Retreive the actual asset data. If the file name is not
        specified use the afid as the file name.
        """
        if fname == None:
            fname = self._ext2upper(afidSuffix)
        fullKey = self._afid2s3key(afidSuffix)
        return self._getBinary(fname, fullKey)

    def articleFiles(self, doiSuffix):
        """
        Download files for all AFIDs associated with this
        DOI. 
        """
        os.mkdir(doiSuffix)
        os.chdir('./'+ doiSuffix)
        result = { doiSuffix : [ (afid, self.assetFile(afid)) for afid in self._afidsFromDoi(doiSuffix) ] }
        os.chdir('../')
        return result 

    def putArticle(self, doiSuffix):
        """
        """
        return
        
if __name__ == "__main__":
    """
    Main entry point for command line execution. 
    """
    import argparse
    import pprint

    # Main command dispatcher.
    dispatch = { 'buckets'      : lambda repo, doiList: repo.bucket_names(),
                 'keycheck'     : lambda repo, doiList: [ repo.keycheck(afid) for afid in doiList ],
                 'articlefiles' : lambda repo, doiList: [ repo.articleFiles(doi) for doi in doiList ],
                 'article'      : lambda repo, doiList: [ repo.article(doi) for doi in doiList ],
                 'articles'     : lambda repo, doiList: repo.articles(),
                 'rm-article'   : lambda repo, doiList: [ repo.rmArticle(doi) for doi in doiList ],
                 'assets'       : lambda repo, doiList: [ repo.assets(doi) for doi in doiList ],
                 'asset'        : lambda repo, doiList: [ repo.asset(doi) for doi in doiList ],
                 'assetall'     : lambda repo, doiList: [ repo.assetall(doi) for doi in doiList ],
               }

    pp = pprint.PrettyPrinter(indent=2)
    parser = argparse.ArgumentParser(description='S3 API client module.')
    parser.add_argument('--bucket', help='specify an S3 buckt to use.')
    parser.add_argument('--prefix', help='specify a DOI prefix.')
    parser.add_argument('command', help="articles, article, articlefiles, assets, asset, assetfile, assetAll, buckets, keycheck")
    parser.add_argument('doiList', nargs='*', help="list of doi's")
    args = parser.parse_args()

    try:
        for val in dispatch[args.command](S3(), args.doiList):
            pp.pprint(val)
    except Exception as e:
        sys.stderr.write('Exception: {msg}.\n'.format(msg=e.message))
        traceback.print_exc(file=sys.stdout)
        sys.exit(1)

    sys.exit(0)
