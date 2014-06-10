#!/usr/bin/env python
"""
PLOS Rhino API Module.

This module exports one class 'Rhino'

"""
from __future__ import print_function
from __future__ import with_statement

import os, sys, traceback, json, re, requests, hashlib, urllib
from StringIO import StringIO
from datetime import datetime

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
    _INGESTIBLES_TMPL = '{server}/{rver}/ingestibles'

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
        self.ingestiblesReq = self._INGESTIBLES_TMPL.format(server=rhinoServer, rver=rver)
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
            return requests.get(url, stream=True, verify=self.verify)
        else:
            return requests.get(url, stream=True)

    def _doPost(self, url, payLoad):
        """
        Requests for Humans not so human after all.
        The verfiy parameter fails if the URL is not https:(
        """
        if url.lower().startswith('https:'):
            return requests.post(url, data=payLoad, stream=True, verify=self.verify)
        else:
            return requests.post(url, data=payLoad, stream=True)

    def _doPatch(self, url, payLoad):
        """
        Requests for Humans not so human after all.
        The verfiy parameter fails if the URL is not https:(
        """
        _payLoad = json.dumps(payLoad)
        _headers = {'content-type': 'application/json'}
        if url.lower().startswith('https:'):
            return requests.patch(url, data=_payLoad, headers=_headers, verify=self.verify)
        else:
            return requests.patch(url, data=_payLoad, headers=_headers)

    def _getBinary(self, fname, url):
        """
        Most of the files other than the article xml are binary in nature.
        Fetch the data and write it to a temporary file. Return the MD5 | SHA1
        hash of the file contents.
        """
        size = 0
        m5 = hashlib.md5()
        s1 = hashlib.sha1()

        r = self._doGet(url)
        if r.status_code == 200:
            with open(fname, 'wb') as f:
                for chunk in r.iter_content(1024*1024,decode_unicode=False):
                    m5.update(chunk)
                    s1.update(chunk)
                    f.write(chunk)
                size = f.tell()
                f.close()
        else:
            return (fname, 'NONE', 'NONE', 'NONE', 0, 'FAILED')
            #raise Exception('rhino:failed to get binary ' + url)
        return (fname, m5.hexdigest(), s1.hexdigest(), r.headers.get('content-type'), size, 'OK')

    def _getMD5(self, url):
        """
        Read from the URL byte by byte calculating
        the MD5 hash. 
        """
        m = hashlib.md5()
        r = self._doGet(url)
        if r.status_code == 200:
           for chunk in r.iter_content(64*1024):
               m.update(chunk)
        else:
           raise Exception('rhino:failed to get MD5 ' + url)
        return m.hexdigest()

    def _getSHA1(self, url):
        """
        Read from the URL byte by byte calculating
        the SHA1 hash. 
        """
        m = hashlib.sha1()
        r = self._doGet(url)
        if r.status_code == 200:
           for chunk in r.iter_content(64*1024):
               m.update(chunk)
        else:
           raise Exception('rhino:failed to get SHA1 ' + url)
        return m.hexdigest()

    def _afidsFromDoi(self, doiSuffix):
        """
        Given a DOI Suffix return a list of AFIDs 
        """
        assets = self.assets(doiSuffix)
        for (adoi, afids) in assets.iteritems():
            for fullAFID in afids:
                (p, afid) = fullAFID.split('/', 1)
                yield afid

    def ingestZipQ(self, zName):
        """
        Given the zip filename 
        """
        r = self._doPost(self.ingestiblesReq, { 'name': zName, 'force_reingest': '' })        
        if r.status_code == 200:
            return json.loads(r.content)
        else:
            raise Exception('rhino:ingestZipQ verb failed ' + r.text)

    def ingestibles(self):
        """
        """
        r = self._doGet(self.ingestiblesReq)
        if r.status_code == 200:
            # Load JSON into a Python object and use some values from it.
            ingestibles = json.loads(r.content)
        else:
            raise Exception('rhino:igestibles verb failed ' + url)
        # Return only doi's matched by filter
        for name in ingestibles:
            yield(name.replace("'",'').encode('utf-8')) 

    def publish(self, doiSuffix):
        """
        """
        if doiSuffix.startswith(self.prefix):
            doiSuffix = self._stripPrefix(doiSuffix, True)

        url = self._ARTICLE_TMPL.format(suffix=doiSuffix)
        r = self._doPatch(url, {'state': 'published'})
        if r.status_code == 200:
            return json.loads(r.content)
        else:
            raise Exception('rhino:publish verb failed ' + r.text)

    def articles(self, useCache=False, stripPrefix=False, lastModified=False):
        """
        List of article DOI's for the given Server and Prefix.
        
        The useCache parameter turns caching of DOIs on or off.
        This is to be used in situations where getting multiple lists 
        of DOIs would slow down processing.
        """
        if useCache and not len(self._ARTICLE_DOI_CACHE) == 0:
            for (k,v) in self._ARTICLE_DOI_CACHE.items():
                yield v 
        else:
            rq = self.articlesReq
            if lastModified:
                rq = rq + '?date'
            r = self._doGet(rq)
            if r.status_code == 200:
                # Load JSON into a Python object and use some values from it.
                articles = json.loads(r.content)
            else:
                raise Exception('rhino:articles verb failed ' + self.articlesReq)
            # Return only doi's matched by filter
            for (doi, doi_lm) in articles.items():
                if self.filterRegx.search(doi):
                    if lastModified:
                        rslt = (self._stripPrefix(doi, stripPrefix), doi_lm['lastModified'])
                    else:
                        rslt = (self._stripPrefix(doi, stripPrefix))
                    if useCache: self._ARTICLE_DOI_CACHE[doi] = rslt
                    yield rslt 

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
                    raise Exception('rhino:invalid prefix ' + fullAFID)
        return result

    def assetFileMD5(self, afidSuffix):
        """
        Return the MD5 hash for the asser identified by
        the afid suffix.
        """
        url = self._ASSETFILES_TMPL.format(afid=afidSuffix)
        return self._getMD5(url)

    def assetFileSHA1(self, afidSuffix):
        """
        Return the SHA1 hash for the asset identified by the 
        afid suffix.
        """
        url = self._ASSETFILES_TMPL.format(afid=afidSuffix)
        return self._getSHA1(url)

    def getAfid(self, afidSuffix, fname=None):
        """
        Retreive the actual asset data. If the name is not 
        specified use the afid as the file name.
        """
        if fname == None:
            fname = urllib.quote(afidSuffix,'')
        url = self._ASSETFILES_TMPL.format(afid=afidSuffix)
        return self._getBinary(fname, url)

    def getXML(self, doi):
        try:
            doiSuffix = self._stripPrefix(doi, True)
            result = self.getAfid(doiSuffix + '.xml')
            print('COMPLETE getXML: ' + doi)
        except:
            result = 'FAILED: ' + doi
            print('FAILED getXML: ' + doi)
   
        return result 
    
    def putAfid(self, afidSuffix, fname, articleMeta, assetMeta, md5, prefix=None,
                  cb=None, reduce_redundancy=True, retry=5, wait=2):    
        """
        """
        raise NotImplementedError('rhino:article not supported') 

    def articleFiles(self, doiSuffix):
        """
        Download files for all AFIDs associated with this DOI. 
        """
        dname = urllib.quote(doiSuffix, '')
        os.mkdir(dname)
        os.chdir('./'+ dname)
        result = { doiSuffix : [ (afid, self.getAfid(afid)) for afid in self._afidsFromDoi(doiSuffix) ] }
        os.chdir('../')
        return result

    def doiMissing(self, dois):
        '''
        Given a list of doi's return a list of 
        of doi's that do not ext on this source. 
        '''
        missing = []
        _dois = dict ((doi, 0) for doi in dois)
        for found_doi in self.articles():
            if _dois.has_key(found_doi):
               _dois[found_doi] = 1
        for k,v in _dois.iteritems():
            if v == 0:
                missing.append(k)
        return missing

if __name__ == "__main__":
    import argparse   
    import pprint 
    from datetime import datetime

    # Main command dispatcher.
    dispatch = { 'articlefiles' : lambda repo, params: [ repo.articleFiles(doi) for doi in params ],
                 'article'      : lambda repo, params: [ repo.article(doi) for doi in params ],
                 'articles'     : lambda repo, _     : repo.articles(lastModified=True),
                 'rm-article'   : lambda repo, params: [ repo.rmArticle(doi) for doi in params ],
                 'assets'       : lambda repo, params: [ repo.assets(doi) for doi in params ],
                 'asset'        : lambda repo, params: [ repo.asset(doi) for doi in params ],
                 'assetall'     : lambda repo, params: [ repo.assetall(doi) for doi in params ],
                 'md5'          : lambda repo, params: [ repo.assetFileMD5(adoi) for adoi in params ],
                 'missing'      : lambda repo, params: repo.doiMissing(params),
                 'ingestibles'  : lambda repo, _     : [ name for name in repo.ingestibles()],
                 'ingest'       : lambda repo, params: [ repo.ingestZipQ(zName) for zName in params ],
                 'publish'      : lambda repo, params: [ repo.publish(doi) for doi in params ],
                 'articlexml'   : lambda repo, params: [ repo.getXML(doi) for doi in params ],
               }

    pp = pprint.PrettyPrinter(indent=2)
    parser = argparse.ArgumentParser(description='Rhino API client module.')
    parser.add_argument('--server', default='http://api.plosjournals.org', help='specify a Rhino server url.')
    parser.add_argument('--prefix', default='10.1371', help='specify a DOI prefix.')
    parser.add_argument('--rver', default='v1', help='rhino version')
    parser.add_argument('--file', default=None, help='File name of alternate input params list.')
    parser.add_argument('command', help='articles | '
                                        'article DOI-SUFFIX | '
                                        'articlefiles DOI-SUFFIX | '
                                        'assets DOI-SUFFIX | '
                                        'asset ASSET-DOI-SUFFIX | '
                                        'assetAll  | '
                                        'md5  | '
                                        'missing | ' 
                                        'ingestibles | ')
    parser.add_argument('params', nargs='*', help="parameter list for commands")
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

    try:
        print('start:' + datetime.now().isoformat())
        r = Rhino(rhinoServer=args.server, prefix=args.prefix, rver=args.rver)
        for val in dispatch[args.command](r, params):
            pp.pprint(val)
        print('finish:' + datetime.now().isoformat())
    except Exception as e:
        sys.stderr.write('Exception: {msg}.\n'.format(msg=e.message.encode('utf-8')))
        traceback.print_exc(file=sys.stdout)
        sys.exit(1)

    sys.exit(0)
        
