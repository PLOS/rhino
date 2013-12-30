#!/usr/bin/env python
"""

"""
from __future__ import print_function
from __future__ import with_statement

import os, sys, traceback, json, re, requests, hashlib, zipfile
from plosapi import Rhino, S3, Currents
from sqlalchemy import create_engine

__author__    = 'Bill OConnor'
__copyright__ = 'Copyright 2013, PLOS'
__version__   = '0.1'

# ***********************************************
#      Text templates used in
#      repetative string creation.
# ***********************************************

# ***** Manifest DTD template *****
DTD_TEXT = """
<!ELEMENT manifest (articleBundle) >

<!ELEMENT articleBundle (article, object*) >

<!-- the article. 'main-entry' specifies the zip entry that contains the nlm
   - article xml; this must match one of the contained representations
   -->
<!ELEMENT article (representation+) >
<!ATTLIST article
    uri         CDATA          #REQUIRED
    main-entry  CDATA          #REQUIRED >

<!-- all included secondary objects (images, movies, data, etc) -->
<!ELEMENT object (representation+) >
<!ATTLIST object
    uri         CDATA          #REQUIRED
    strkImage   CDATA          #IMPLIED          >

<!-- a specific representation.
   - 'name' is the name (label) to store this representation under;
   - 'entry' specifies the entry in the zip that contains this representation
   -->
<!ELEMENT representation EMPTY >
<!ATTLIST representation
    name        CDATA          #REQUIRED
    entry       CDATA          #REQUIRED >
""".lstrip()

# ***** Skeletal Manifest Template *****
MANIFEST_TMPL = """<?xml version="1.1"  encoding="UTF-8"?>
<!DOCTYPE manifest SYSTEM "manifest.dtd">
<manifest>
  <articleBundle>
    {article}
    {objects}
  </articleBundle>
</manifest>
""".lstrip()

# ***** Manifest Object Entries *****
OBJECT_TMPL = """     <object uri="{uri}" {strkimage}>
        {reps}
     </object>"""

# ***** Object Representation Template *****
REPRESENTATION_TMPL = """<representation name="{ext}" entry="{filename}" />"""

# ***** Manifest Article Template *****
ARTICLE_TMPL = """
     <article uri="{uri}" main-entry="{fname}">
        {reps}
     </article>"""

# ***** URI Template *****
URI_TMPL = """info:doi/{doi}"""


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

def _stripPrefix(doi, srcRepo):
    return doi.replace(srcRepo.prefix + '/','')

def _stripJournal(s):
    s = s.replace('10.1371/','')
    return s.replace('journal.','')

def _extractNameExt(s):
    s_split = s.split('.')
    return (_stripJournal(s), s_split[len(s_split)-1])


def _representationTags(afids):
    """
    Build a set of representation entities and tuples with the name, uri
    and extension of the the asset.
    """
    repsTags = []
    for afid in afids:
        (fname, ext) = _extractNameExt(afid)
        repsTags.append(REPRESENTATION_TMPL.format(ext=ext, filename=fname))
    return '\n        '.join(repsTags)

def _manifestInfo(doi, srcRepo):
    """
    """
    doiSuffix = _stripPrefix(doi, srcRepo)
    article = srcRepo.article(doiSuffix)
    assets = article['assets']
        
    # Get the XML and PDF representations
    for afid in assets[doi]:
        _afid = _stripJournal(afid)
        if _afid.lower().endswith('.xml'):
            xml_afid = _afid
        elif _afid.lower().endswith('.pdf'):
            pdf_afid = _afid
    
    # Remove the XML and PDF representations
    # from all the rest.
    del assets[doi]

    return { 'URI' : URI_TMPL.format(doi=doi),
             'doi' : doi,
             'prefix' : srcRepo.prefix,
             'xml_afid' : xml_afid,
             'pdf_afid' : pdf_afid,
             'strkImageURI' : article.get('strkImgURI', ''),
             'assets' : assets
            }

def _objectTags(manifestInfo):
    """
    Build a set of object entities and collect a list of tuples containing
    the name, uri and extension of the the respresentaions of that object.
    """
    objTags = []
    strkImage = manifestInfo.get('strkImageURI')
    
    for (adoi, afids) in manifestInfo['assets'].iteritems():
        auri =  URI_TMPL.format(doi=adoi)
        strkValue = 'strkImage="True"' if strkImage == auri else ''
        repTags = _representationTags(afids)
        objTags.append(OBJECT_TMPL.format(uri=auri, strkimage=strkValue,reps=repTags))
    return '\n'.join(objTags)

def _articleTags(mi):
    """
    Build a Article entity with the the XML and PDF representation tags
    included.
    """
    xmlRepTag = REPRESENTATION_TMPL.format(ext='XML', filename=mi['xml_afid'])
    pdfRepTag = REPRESENTATION_TMPL.format(ext='PDF', filename=mi['pdf_afid'])
    return ARTICLE_TMPL.format(uri=mi['URI'], fname=mi['xml_afid'], reps=xmlRepTag + '\n        ' + pdfRepTag)

def _manifestXML(doi, srcRepo):
    """
    Build the manifest for the specified article.
    """
    mi = _manifestInfo(doi, srcRepo)
    articleTags = _articleTags(mi)
    objectTags = _objectTags(mi)
    return MANIFEST_TMPL.format(article=articleTags, objects=objectTags)

def repackage(dois, srcRepo, apd):
    """
    Repackage an article as a zip file for ingestion.
    """
    for doi in dois:
        doiSuffix = _stripPrefix(doi, srcRepo)
        manifestXML = _manifestXML(doi, srcRepo) 
        srcRepo.articleFiles(doiSuffix)
        with zipfile.ZipFile(_stripJournal(doiSuffix) + '.zip', mode='w') as zf:
            zf.writestr('manifest.dtd', DTD_TEXT, compress_type=zipfile.ZIP_DEFLATED)
            zf.writestr('MANIFEST.xml', manifestXML, compress_type=zipfile.ZIP_DEFLATED)
            for (dpath, _, fnames) in os.walk(doiSuffix):
                for fname in fnames:
                    fullFile = '{p}/{f}'.format(p=dpath, f=fname)            
                    apd.append('Adding: ' + fullFile)
                    zf.write(fullFile, _stripJournal(fname), compress_type=zipfile.ZIP_DEFLATED)
            zf.close()


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
                                        'backup  [no params] | \n'
                                        'repackage')
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
                   lambda src, dst, apd: backup(src, dst, apd),
                 'repackage'    :
                   lambda src, dst, apd: repackage(args.params, src, apd), }

    src = Rhino(rhinoServer=args.server, prefix=args.prefix)
    dst = S3(bucketID=args.bucket, prefix=args.prefix)
    apd = print_appender()
    dispatch[args.command](src, dst, apd)
    sys.exit(0)
