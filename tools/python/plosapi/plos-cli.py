#!/usr/bin/env python
"""

"""
from __future__ import print_function
from __future__ import with_statement

import os, sys, traceback, json, re, requests, md5

__author__    = 'Bill OConnor'
__copyright__ = 'Copyright 2013, PLOS'
__version__   = '0.1'

from plosapi import Rhino, S3, Currents

class print_appender():
    """
    """
    def __init__(self):
        """
        """
        import pprint
        self.pp = pprint.PrettyPrinter(indent=2)
        return

    def append(self, obj):
        """
        """
        self.pp.pprint(obj)
        return []

class list_appender():
    """
    """
    def __init__(self, lst):
        """
        """
        self.lst = []
        return

    def append(self, obj):
        """
        """
        self.lst.append(obj)
        return self.lst

def assetDiff(doiSuffixes, srcRepo, dstRepo, matchOnly=''):
    """
    
    """
    for doiSuffix in doiSuffixes:
        srcAssets = srcRepo.assets(doiSuffix)
        dstAssets = dstRepo.assets(doiSuffix)
        diffs = {}
        for adoi,afids in srcAssets.iteritems():
            for afid in afids:
                k = '{afid}'.format(adoi=adoi, afid=afid)
                diffs[k] = '+-'

        for adoi,afids in dstAssets.iteritems():
            for afid in afids:
                k = '{afid}'.format(adoi=adoi, afid=afid)
                if diffs.has_key(k):
                    diffs[k] = '++'
                else:
                    diffs[k] = '-+'
    
        for k,v in diffs.iteritems():
            if not matchOnly == '':
               if v == matchOnly:
                   yield k
            else:
               yield (k, v)

def doiDiff(srcRepo, dstRepo, matchOnly=''):
    """
    Basic SourceRepo DestinationRepo DOI diff.
    Even if both the source and destination 
    have the DOI it is not necessarily the case 
    that all the assets for the DOI exist on 
    both. A deeper verify would need to be done.
    
    The result of a diff can be one of 3 states.

    '++' - DOI exists on both source and destination
    '+-' - DOI exists on source but not destination
    '-+' - DOI not on source but on destination
    ''   - match any of the above.
    """
    diffs = {}
    # Assume source only 
    for doi in src.articles():
        diffs[doi] = '+-'
    # Flag them as existing on both 
    # or just destination 
    for doi in dst.articles():
        if diffs.has_key(doi):
            diffs[doi] = '++'
        else:
            diffs[doi] = '-+'
    # Matching filter
    for k,v in diffs.iteritems():
        if not matchOnly == '':
           # Output only matches
           if v == matchOnly:
               yield k
        else:
           # Output everything 
           yield (k, v)

def _afidCopy(afidSuffix, srcRepo, dstRepo, articleMData, assetMData):
    """
    Private AFID copy.  
    """
    try:
        # Download the source file
        (fname, md5) = srcRepo.getAfid(afidSuffix)
        ( _, _, _, status) = dstRepo.putAfid(afidSuffix, fname, articleMData, assetMData, md5, cb=None)
    except Exception as e:
        status = 'FAILED: {adoi} {msg}'.format(adoi=afidSuffix, msg=e.message)
        fname = '' 
        md5 = ''
    else:
        # If there are no exceptions - complete
        os.remove(fname)
    return (afidSuffix, fname, md5, status) 
    
def adoiCopy(doiSuffix, srcRepo, dstRepo, articleMData):
    """
    """
    # Get adoi's as keys and list of afid's as values
    srcAssets = srcRepo.assets(doiSuffix)
    for adoi,afids in srcAssets.iteritems():
        adoiSuffix = adoi.replace(srcRepo.prefix + '/', '')
        # Asset returns a key value pair where 
        # key = afid  and  value = assetmeta-data
        for afid,assetMData in srcRepo.asset(adoiSuffix).iteritems():
            afidSuffix = afid.replace(srcRepo.prefix + '/', '')
            # copy to the destination and yield the result
            yield _afidCopy(afidSuffix, srcRepo, dstRepo, articleMData, assetMData)

def doiCopy(doiSuffixes, srcRepo, dstRepo, appender):
    """
    Basic DOI copy cli command. Copy each asset associated with
    the DOI from SourceRepo to DestinationRepo. A temporary
    directory named doiSuffix is created. Asset files are
    temporarily downloaded and uploaded to the destination. 
    """
    for doiSuffix in doiSuffixes:
        appender.append('Copying {doi}'.format(doi=doiSuffix))
        # Get the meta-data before making the directory
        # If an exception is thrown we will not have to clean up.
        articleMData = srcRepo.article(doiSuffix)
        os.mkdir(doiSuffix)
        os.chdir('./'+ doiSuffix)

        for copy_result in adoiCopy(doiSuffix, srcRepo, dstRepo, articleMData):
           appender.append(copy_result)

        os.chdir('../')
        os.removedirs(doiSuffix) 
    return  
    
def backup(srcRepo, dstRepo, appender):
    """
    Basic backup command. Start with a list of ODI's that
    are on the SourceRepo and copy all the assets to the 
    DestinationRepo.
    """
    for doi in doiDiff(srcRepo, dstRepo, matchOnly='+-'):
        # Not interested in prefix - remove it.
        doiSuffix = doi.replace(srcRepo.prefix + '/', '')
        # Fake passing in a list of DOI's because doiCopy
        # is also a cli command and expects that.
        doiCopy([ doiSuffix ], srcRepo, dstRepo, appender)
    return 

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description='CLI for backup management of corpus. The Rhino server is the default'
                                                 ' source and S3 is the default destination. ')
    parser.add_argument('--server',  default='http://api.plosjournals.org', 
                         help='specify a Rhino server url. Default: %(default)s')
    parser.add_argument('--bucket', default='us-west-1.pub.plos.org', 
                         help='specifiy the S3 bucket. Default: %(default)s')
    parser.add_argument('--prefix', default='10.1371', help='specify a DOI prefix.  Default: %(default)s')
    parser.add_argument('--matchOnly', metavar='"+-"', default='+-', 
                         help='source and destination matching criteria. [-+, +-, ++, ""]')
    parser.add_argument('command', help='doidiff [no params] | '
                                        'assetdiff [doi suffixes] | ' 
                                        'backup  [no params]')
    parser.add_argument('params', nargs='*', help="command dependent parameters")
    args = parser.parse_args()

    # Main command dispatcher.
    dispatch = { 'doidiff'   : lambda src, dst, apd: [apd.append(rslt) for rslt in doiDiff(src, dst, matchOnly=args.matchOnly)], 
                 'assetdiff' : lambda src, dst, apd: [apd.append(rslt) for rslt in assetDiff(args.params, src, dst, matchOnly=args.matchOnly)],
                 'doicopy'   : lambda src, dst, apd: doiCopy(args.params, src, dst, apd) ,
                 'backup'    : lambda src, dst, apd: backup(src, dst, apd) }

    src = Rhino(rhinoServer=args.server, prefix=args.prefix)
    dst = S3(bucketID=args.bucket, prefix=args.prefix)
    try:
        apd = print_appender()
        dispatch[args.command](src, dst, apd)
    except Exception as e:
        sys.stderr.write('Exception: {msg}.\n'.format(msg=e.message))
        traceback.print_exc(file=sys.stdout)
        sys.exit(1)
    sys.exit(0)
