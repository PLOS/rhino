#!/usr/bin/env python
"""

"""
from __future__ import print_function
from __future__ import with_statement

import os, sys, traceback, json, re, requests, hashlib 
from plosapi import Rhino, S3, Currents
from sqlalchemy import create_engine

__author__    = 'Bill OConnor'
__copyright__ = 'Copyright 2013, PLOS'
__version__   = '0.1'

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
        diffs = dict() 
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
    diffs = dict()
    # Assume source only
    for doi in srcRepo.articles():
        diffs[doi] = '+-'
    # Flag them as existing on both 
    # or just destination
    for doi in dstRepo.articles():
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
           # matchOnly == ''
           # Output everything 
           yield (k, v)

def deepDiff(srcRepo, dstRepo, appender, matchOnly='+-'):
    for doi in srcRepo.articles(stripPrefix=True):
        try:
            for afid in assetDiff([doi], srcRepo, dstRepo, matchOnly):
                appender.append(afid)
        except Exception as e:
            appender.append(e.message)
    return
            
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
    finally:
        # If it failed to get the file there will be an exception
        # so cleanup if necessary.i getAfid defaults to using
        # afidSuffix as the file name.
        if os.path.exists(afidSuffix):
            os.remove(afidSuffix)
    return (afidSuffix, fname, md5, status) 
    
def adoiCopy(doiSuffix, srcRepo, dstRepo, articleMData):
    """
    """
    # Get adoi's as keys and list of afid's as values
    srcAssets = srcRepo.assets(doiSuffix)
    for adoi,afids in srcAssets.iteritems():
        adoiSuffix = adoi.replace(srcRepo.prefix + '/', '')
        # Asset returns a key value pair where 
        # key = afid  and  value = asset meta-data
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
        try:
            articleMData = srcRepo.article(doiSuffix)
            os.mkdir(doiSuffix)
            os.chdir('./'+ doiSuffix)
            for copy_result in adoiCopy(doiSuffix, srcRepo, dstRepo, articleMData):
                # Result Tuple (afidSuffix, fname, md5, status)
                appender.append(copy_result)
        except:
            appender.append('Copy Failed {doi}'.format(doi=doiSuffix))
        finally:
            os.chdir('../')
            if os.path.exists(doiSuffix):
                os.removedirs(doiSuffix) 
    return  
    
def backup(srcRepo, dstRepo, appender):
    """
    Basic backup command. Start with a list of DOI's that
    are on the SourceRepo and copy all the assets to the 
    DestinationRepo.

    Since we are doing a backup we will only diff with 
    what is available on the source repo. 
    """
    # Turn the cache on to prevent fetching DOI list twice.
    srcArticles = srcRepo.articles(useCache=True, stripPrefix=True)
 
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
    parser.add_argument('--file', default=None, help='File name of alternate input params list.')
    parser.add_argument('command', help='doidiff [no params] | '
                                        'doicopy [doi suffixes] | '
                                        'assetdiff [doi suffixes] | ' 
                                        'backup  [no params]')
    parser.add_argument('params', nargs='*', help="command dependent parameters")
    args = parser.parse_args()
    params = args.params

    # If --file is true get what would normally
    # be params on the command line from a file
    # where each line is a separate parameter.
    if args.file:
        fp = open(args.file, 'r')
        params = []
        for p in fp:
            params.append(p.strip())
        fp.close()

    # Main command dispatcher.
    dispatch = { 'doidiff'   : 
                   lambda src, dst, apd: [apd.append(rslt) for rslt in doiDiff(src, dst, matchOnly=args.matchOnly)],
                 'deepdiff' :
                   lambda src, dst, apd: deepDiff(src, dst, apd, matchOnly=args.matchOnly),  
                 'assetdiff' : 
                   lambda src, dst, apd: [apd.append(rslt) for rslt in assetDiff(params, src, dst, matchOnly=args.matchOnly)],
                 'doicopy'   : 
                   lambda src, dst, apd: doiCopy(params, src, dst, apd) ,
                 'backup'    : 
                   lambda src, dst, apd: backup(src, dst, apd) }

    src = Rhino(rhinoServer=args.server, prefix=args.prefix)
    dst = S3(bucketID=args.bucket, prefix=args.prefix)
    apd = print_appender()
    dispatch[args.command](src, dst, apd)
    sys.exit(0)
