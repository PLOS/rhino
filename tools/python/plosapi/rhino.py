#!/usr/bin/env python
"""
PLOS Rhino API Module.

This module exports one class 'Rhino'

"""
from __future__ import print_function
from __future__ import with_statement

import os, sys, traceback, json, re, requests, md5

__author__    = 'Bill OConnor'
__copyright__ = 'Copyright 2013, PLOS'
__version__   = '0.1'

class Rhino:
    """
    Class Rhino is the python client for PLOS's Open Access publishing system API. 
    """
    _DOI_TMPL        = '{prefix}/'
    _ARTICLES_TMPL   = '{server}/{rver}/articles'
    _ARTICLE_TMPL    = '{server}/{rver}/articles/{prefix}/'
    _ASSETS_TMPL     = '{server}/{rver}/assets/{prefix}/'
    _ASSETFILES_TMPL = '{server}/{rver}/assetfiles/{prefix}/'
    _ARCTILE_DOI_CACHE = dict()
    
    def __init__(self, rhinoServer='http://api.plosjournals.org/', prefix='10.1371', 
                     rver='v1', verify=False, regx='.*'):
        self.rhinoServer = rhinoServer
        self.prefix      = prefix
        self.verify      = verify
        self.rver        = rver

        """
        If no filter is specified in the options
        then match on everything.
        """
        self.filterRegx = re.compile(regx)
        self.articlesReq = self._ARTICLES_TMPL.format(server=rhinoServer, rver=rver)
        """
        Maybe this is bad form but we want to initialize the template as
        much as possible and only once. We will have to format in the DOI
        when the actual request is made.
        """
        self._DOI_TMPL = self._DOI_TMPL.format(prefix=prefix) + '{suffix}'
        self._ASSETS_TMPL = \
          self._ASSETS_TMPL.format(server=rhinoServer, rver=rver, prefix=prefix) + '{adoi}'
        self._ASSETFILES_TMPL = \
          self._ASSETFILES_TMPL.format(server=rhinoServer, rver=rver, prefix=prefix) + '{afid}'
        self._ARTICLE_TMPL = \
          self._ARTICLE_TMPL.format(server=rhinoServer, rver=rver, prefix=prefix) + '{suffix}'

    def _stripPrefix(self, doi, strip):
        """
        """
        if not strip: return doi
        return doi.replace('{p}/'.format(p=self.prefix), '')

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
        return (fname, m.hexdigest())

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

    def _afidsFromDoi(self, doiSuffix):
        """
        Given a DOI Suffix return a list of AFIDs 
        """
        assets = self.assets(doiSuffix)
        for (adoi, afids) in assets.iteritems():
            for fullAFID in afids:
                (p, afid) = fullAFID.split('/')
                yield afid

    def articles(self, useCache=False, stripPrefix=False):
        """
        List of article DOI's for the given Server and Prefix.
        
        The useCache parameter turns caching of DOIs on or off.
        This is to be used in situations where getting multiple lists 
        of DOIs would slow down processing.
        """
        if useCache and not len(self._ARTICLE_DOI_CACHE) == 0:
            for k in self._ARTICLE_DOI_CACHE.keys():
                yield self._stripPrefix(k, stripPrefix)
        else:
            r = self._doGet(self.articlesReq)
            if r.status_code == 200:
                # Load JSON into a Python object and use some values from it.
                articles = json.loads(r.content)
            else:
                raise Exception('rhino:articles verb failed ' + url)
            # Return only doi's matched by filter
            for (doi, _) in articles.items():
                if self.filterRegx.search(doi):
                    if useCache: self._ARTICLE_DOI_CACHE[doi] = 1
                    yield(self._stripPrefix(doi, stripPrefix)) 
    
    def article(self, doiSuffix):
        """
        Get the meta-data associated with the artile DOI.
        """
        url = self._ARTICLE_TMPL.format(suffix=doiSuffix)
        r = self._doGet(url)

        if r.status_code == 200:
            # Load JSON into a Python object and use some values from it.
            article = json.loads(r.content)
        else:
            raise Exception('rhino:article meta-data not found ' + url)
        return article

    def rmArticle(self, doiSuffix):
        """
        Not implemented for Rhino
        """
        #for afid in self._afidsFromDoi(doiSuffix):
        #    print(afid)
        raise NotImplemented('rhino: not implemented for fhino')
        return

    def assets(self, doiSuffix):
        """
        Each article has one or more assets. Each asset has its
        own DOI. Each asset DOI can represent data of one or more
        representations. Currently our "representations" are psycho
        because they are often just mimetypes and sometimes just 
        made up and the mimetype can be inferred. 

        For example: prefix/journal.pone.XXXXXXX usually has 2 
        repesentations. journal.pone.XXXXXXX.xml and journal.pone.XXXXXXX.pdf

        prefix/journal.pone.XXXXXXX.g00001 can have several 
        representations journal.pone.XXXXXXX.g00001.tif and
        journal.pone.XXXXXXX.g00001.PNG_M (which is a medium sized png)  
        
        This method returns a dict with the asset DOI's
        as keys. Each key indexes a list of representations for
        the asset DOI. The item in the list of identifiers will be known
        as the asset file identifiers or afid's.
        """
        article = self.article(doiSuffix)
        assets = dict((asset_doi, asset_afids.keys())
                  for (asset_doi, asset_afids) in article['assets'].items())
        return assets 

    def asset(self, adoiSuffix):
        """
        Each article is represent by a DOI of the form
        ex. 10.1371/journal.pone.XXXXXXXX. By default this
        is technically an asset doi also. There are generally
        2 representations associated with this article/asset DOI -
        the XML and the PDF or the article.  There are typically
        other assets associated with the article such as tables
        and suplementary data. For exampel an asset DOI for a 
        table will look like: 10.1371/journal.pone.XXXXXXX.t0001.
        
        There may be one or more representations for an asset
        for example a TIF, PNG or CSV data. Each asset DOI 
        will have a list of asset file identitifiers(afid) and meta-data
        associated with each.

        The method expects an asset DOI as input. It returns a
        dict with AFID's as keys and AFID meta-data as values.

        WTO NOTE: This list could be had from information returned
                  in article queries but it is handy and transfers
                  less data. 
        """
        url = self._ASSETS_TMPL.format(adoi=adoiSuffix)
        r = self._doGet(url)

        if r.status_code == 200:
            # Load JSON into a Python object and use some values from it.
            asset = json.loads(r.content)
        else:
            raise Exception('rhino:asset meta-data not found ' + url)
        return asset    

    def assetall(self, adoiSuffix):
        """
        """
        assets = self.assets(adoiSuffix)
        result = dict()
        for (doi, afids) in assets.iteritems():
            for fullAFID in afids:
                afid = fullAFID.split('/')
                if afid[0] == self.prefix:
                    afidSuf = '.'.join(afid[1].split('.')[:-1])
                    result.update(self.asset(afidSuf))
                else:
                    raise Exception('rhino:invalid s3 prefix ' + fullAFID)
        return result

    def assetFileMD5(self, afidSuffix):
        """
        """
        url = self._ASSETFILES_TMPL.format(afid=afidSuffix)
        return self._getMD5(url)

    def getAfid(self, afidSuffix, fname=None):
        """
        Retreive the actual asset data. If the name is not 
        specified use the afid as the file name.
        """
        if fname == None:
            fname = afidSuffix
        url = self._ASSETFILES_TMPL.format(afid=afidSuffix)
        return self._getBinary(fname, url)
    
    def putAfid(self, afidSuffix, fname, articleMeta, assetMeta, md5, prefix=None,
                  cb=None, reduce_redundancy=True, retry=5, wait=2):    
        """
        """
        raise NotImplementedError('rhino:article not supported') 

    def articleFiles(self, doiSuffix):
        """
        Download files for all AFIDs associated with this DOI. 
        """
        os.mkdir(doiSuffix)
        os.chdir('./'+ doiSuffix)
        result = { doiSuffix : [ (afid, self.getAfid(afid)) for afid in self._afidsFromDoi(doiSuffix) ] }
        os.chdir('../')
        return result

if __name__ == "__main__":
    import argparse   
    import pprint 
 
    # Main command dispatcher.
    dispatch = { 'articlefiles' : lambda repo, doiList: [ repo.articleFiles(doi) for doi in doiList ],
                 'article'      : lambda repo, doiList: [ repo.article(doi) for doi in doiList ],
                 'articles'     : lambda repo, doiList: repo.articles(),
                 'rm-article'   : lambda repo, doiList: [ repo.rmArticle(doi) for doi in doiList ],
                 'assets'       : lambda repo, doiList: [ repo.assets(doi) for doi in doiList ],
                 'asset'        : lambda repo, doiList: [ repo.asset(doi) for doi in doiList ],
                 'assetall'     : lambda repo, doiList: [ repo.assetall(doi) for doi in doiList ],
                 'md5'          : lambda repo, doiList: [ repo.assetFileMD5(adoi) for adoi in doiList ],
               }

    pp = pprint.PrettyPrinter(indent=2)
    parser = argparse.ArgumentParser(description='Rhino API client module.')
    parser.add_argument('--server', help='specify a Rhino server url.')
    parser.add_argument('--prefix', help='specify a DOI prefix.')
    parser.add_argument('command', help="articles, article, articlefiles, assets, asset, assetfile, assetAll, md5")
    parser.add_argument('doiList', nargs='*', help="list of doi's")
    args = parser.parse_args()

    try:
        for val in dispatch[args.command](Rhino(), args.doiList):
            pp.pprint(val)
    except Exception as e:
        sys.stderr.write('Exception: {msg}.\n'.format(msg=e.message))
        traceback.print_exc(file=sys.stdout)
        sys.exit(1)

    sys.exit(0)
        
    
