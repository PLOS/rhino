#!/usr/bin/python2

"""
    Scrape article XMLs and package them for ingestion.

    Usage: repackage.py rhino_url prefix article_name

    Note:
        rhino_url - is the usl to the rhino SOA for the system you are interested in.
        prefix    - the doi prefix (for PLOS 10.1371)
        article_name - the first part of the article zip name.

   Ex. repackage.py --server="https://webprod.plosjournals.org/api"  "pone.0033205"
"""

from __future__ import print_function
from __future__ import with_statement
from cStringIO import StringIO
from optparse import OptionParser

import itertools
import json
import os
import re
import requests
import string
import sys
import zipfile


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
OBJECT_TMPL = """    <object uri="{uri}" {strkimage}>
        {reps}
     </object>"""

# ***** Object Representation Template *****
REPRESENTATION_TMPL = """<representation name="{name}" entry="{entry}" />"""

# ***** Manifest Article Template *****
ARTICLE_TMPL = """
     <article uri="{uri}" main-entry="{fname}">
        {reps}
     </article>"""

# ***** URI Template *****
URI_TMPL = """info:doi/{name}"""

# ***** DOI Template *****
DOI_TMPL = """{prefix}/journal.{name}"""

FETCH_ARTICLE_DOI_TMPL = '{server}/{rver}/articles'
FETCH_ARTICLE_INFO_TMPL = '{server}/{rver}/articles/{doi}'
FETCH_ASSETS_RHINO_TMPL = '{server}/{rver}/assets/{doi}'
FETCH_FILE_RHINO_TMPL = '{server}/{rver}/assetfiles/{afid}'
FETCH_FILE_AMBRA_TMPL = '{server}/{rver}/article/fetchObject.action?uri=info:doi/{doi}&representation={ext}'

DOI_PATTERN = re.compile(r'(\d+\.\d+/)?(journal\.)?(.*)\.([^.]*)')

# *****************************************************
def doGet(url, verify=False):
    """
    Requests for Humans is not so human after all.
    The verfiy parameter fails if the URL is not https:(
    """
    if url.lower().startswith('https:'):
        return requests.get(url, verify=verify)
    else:
        return requests.get(url)

def fetchArticleList(options):
    """
    Fetch the list of article PLOS identifiers 
    for the given Server and Prefix.
    """
    (base_url, prefix, rver) = (options.rhinoServer, options.prefix, options.rver)
    url = FETCH_ARTICLE_DOI_TMPL.format(server=base_url, rver=rver)
    r = doGet(url, verify=options.verify)
    if r.status_code == 200:    
        # Load JSON into a Python object and use some values from it.
        articleList = json.loads(r.content)
    else:
        raise Exception("Article list failed  " + url)
    return [ doi for (doi, _) in articleList.items() ] 

def fetchArticleMeta(name, options):
    """
    Fetch the article meta-data.
       -name : the article FSID ex. pone.1234567
    """
    (base_url, prefix, rver) = (options.rhinoServer, options.prefix, options.rver)
    doi = DOI_TMPL.format(prefix=prefix, name=name)
    url = FETCH_ARTICLE_INFO_TMPL.format(server=base_url, rver=rver, doi=doi)
    r = doGet(url, verify=options.verify)
    if r.status_code == 200:
        # Load JSON into a Python object and use some values from it.
        metaData = json.loads(r.content)
    else:
        raise Exception("Article meta-data not found  " + url)
    return metaData

def fetchAssetMeta(options, doi=None, afid=None):
    """
    Given an Asset ID or an Asset DOI return
    the meta-data 
    """
    (base_url, prefix, rver) = (options.rhinoServer, options.prefix, options.rver)
    if (doi == None):
        doi = DOI_TMPL.format(prefix=prefix, name=afid)
    url = FETCH_ASSETS_RHINO_TMPL.format(server=base_url, rver=rver, doi=doi)
    r = doGet(url, verify=options.verify)
    if r.status_code == 200:
        # Load JSON into a Python object and use some values from it.
        metaData = json.loads(r.content)
    else:
        raise Exception("Asset meta-data not found  " + url)
    return  metaData 

def fetchAssetFiles(name, options):
    """
    """
    article = fetchArticleMeta(name, options)
    assetFiles = dict((asset_name, asset_files.keys())
                  for (asset_name, asset_files) in article['assets'].items())
    return assetFiles

def fetchManifestInfo(name, options):
    """
    Fetch the information necessary to build a manifest from Rhino.
    """
    (base_url, prefix, rver) = (options.rhinoServer, options.prefix, options.rver)
    doi = DOI_TMPL.format(prefix=prefix, name=name)
    uri = URI_TMPL.format(name=doi)

    # Load JSON into a Python object and use some values from it.
    article = fetchArticleMeta(name, options)
    assets = dict((asset_name, asset_files.keys())
                  for (asset_name, asset_files) in article['assets'].items())

    xml_asset = None
    pdf_asset = None
    if doi in assets:
        for root_asset in assets[doi]:
            if root_asset.upper().endswith('.XML'):
                xml_asset = root_asset
            if root_asset.upper().endswith('.PDF'):
                pdf_asset = root_asset
        del assets[doi]

    return { 'URI' : uri,
             'doi' : doi,
             'prefix' : prefix,
             'xml_file' : '{name}.{ext}'.format(name=name, ext='xml'),
             'pdf_file' : '{name}.{ext}'.format(name=name, ext='pdf'),
             'xml_asset' : xml_asset,
             'pdf_asset' : pdf_asset,
             'strkImageURI' : article.get('strkImgURI', ''),
             'assets' : assets
            }

_BANNER_WIDTH = 79

def pretty_dict_repr(d):
    """Represent a dictionary with human-friendly text.

    Assuming d is of type dict, the output should be syntactically
    equivalent to repr(d), but with each key-value pair on its own,
    indented line.
    """
    lines = ['    {0!r}: {1!r},'.format(k, v) for (k, v) in sorted(d.items())]
    return '\n'.join(['{'] + lines + ['}'])

def section(*parts):
    """Print a section banner."""
    print('=' * _BANNER_WIDTH)
    print(*parts)
    print()

def report(description, response):
    """Print a description of the HTTP response."""
    print('-' * _BANNER_WIDTH)
    print(description)
    print()
    print('Status {0}: {1!r}'.format(response.status_code, response.reason))
    print('Headers:', pretty_dict_repr(response.headers))
    print()

    print('Response size:', len(response.content))
    content_lines = list(response.iter_lines())
    for (line_number, line) in enumerate(content_lines):
        if line_number > 24:
            print('...')
            print(content_lines[-1])
            break
        print(line)
    print()

def fetchBinary(filename, url):
    """
    Most of the files other than the article xml is binary in nature. Fetch
    the data and write it to a temporary file.
    """
    r = doGet(url)
    if r.status_code == 200:
        with open(filename, 'wb') as f:
            for chunk in r.iter_content(1024):
                f.write(chunk)
    else:
        raise Exception("not downloaded  " + url)

def make_zip(options, name, manifest, md):
    """
    Take the information we have gather about the article so far and create
    the zip.
    """
    (base_url, prefix, rver) = (options.rhinoServer, options.prefix, options.rver)
    xml_asset_url = FETCH_FILE_RHINO_TMPL.format(server=base_url, rver=rver, afid=md['xml_asset'])
    fetch = doGet(xml_asset_url, verify=False)
    if fetch.status_code != 200:
        raise Exception("Could not retrieve XML")
    zip_path = os.path.join('.', name + '.zip')
    with zipfile.ZipFile(zip_path, mode='w') as zf:
        zf.writestr('MANIFEST.xml', manifest, compress_type=zipfile.ZIP_DEFLATED)
        zf.writestr(md['xml_file'], fetch.content, compress_type=zipfile.ZIP_DEFLATED)
        zf.writestr('manifest.dtd', DTD_TEXT, compress_type=zipfile.ZIP_DEFLATED)

        url = FETCH_FILE_RHINO_TMPL.format(server=base_url, rver=rver, afid=md['pdf_asset'])
        fetchBinary(md['pdf_file'], url)
        zf.write(md['pdf_file'], md['pdf_file'] , compress_type=zipfile.ZIP_DEFLATED)
        os.remove(md['pdf_file'])

        asset_file_ids = itertools.chain(*md['assets'].values())
        for asset_file_id in asset_file_ids:
            assetName, ext = infer_filename(asset_file_id)
            url = FETCH_FILE_RHINO_TMPL.format(server=base_url, rver=rver, afid=asset_file_id)
            name = '{name}.{ext}'.format(name=assetName, ext=ext.lower())
            fetchBinary(name, url)
            zf.write(name, name , compress_type=zipfile.ZIP_DEFLATED)
            os.remove(name)

def infer_filename(asset_file_id):
    """Infer the PLOS-form file name and extension from the DOI.

    TODO: Generify
    """
    g = DOI_PATTERN.match(asset_file_id).groups()
    return g[2], g[3]

def buildReps(name, asset_file_ids, prefix='10.1371'):
    """
    Build a set of representation entities and tuples with the name, uri
    and extension of the the asset.
    """
    repsTags = []
    for asset_file_id in asset_file_ids:
        name, ext = infer_filename(asset_file_id)
        fn = '{name}.{ext}'.format(name=name, ext=ext.lower())
        repsTags.append(REPRESENTATION_TMPL.format(name=ext,entry=fn))
    reps = ('\n' + (8 * ' ')).join(repsTags)
    return reps

def buildObjectTags(md):
    """
    Build a set of object entities and collect a list of tuples containing
    the name, uri and extension of the the respresentaions of that object.
    """
    objects = []
    objectTuples = []
    strkImage = md.get('strkImageURI')
    for (asset_id, asset_file_ids) in md['assets'].items():
        asset_uri =  URI_TMPL.format(prefix=md['prefix'], name=asset_id)
        strkValue = 'strkImage="True"' if strkImage == asset_uri else ''
        reps = buildReps(asset_id, asset_file_ids)
        objects.append(OBJECT_TMPL.format(uri=asset_uri, strkimage=strkValue,reps=reps))
    return '\n'.join(objects)

def buildArticleTag(md):
    """
    Build a Article entity with the the XML and PDF representation tags
    included.
    """
    xmlRep = REPRESENTATION_TMPL.format(name='XML', entry=md['xml_file'])
    pdfRep = REPRESENTATION_TMPL.format(name='PDF', entry=md['pdf_file'])
    return ARTICLE_TMPL.format(uri=md['URI'], fname=md['xml_file'], reps=xmlRep + "\n        " + pdfRep)

def build_manifest_xml(md):
    """
    Build the manifest for the specified article.
    """
    articleTag = buildArticleTag(md)
    objectTags = buildObjectTags(md)
    return MANIFEST_TMPL.format(article=articleTag, objects=objectTags)

def cmd_repackage(options, args):
    failedArticles = []
    for name in args:
        try:
            print('Fetch manifest')
            manifest_dict = fetchManifestInfo(name=name, options=options)
            print(manifest_dict)
            print('Build manifest')
            manifest = build_manifest_xml(manifest_dict)
            print('Zip it up {s}  {n}'.format(s=options.rhinoServer, n=name))
            make_zip(options, name, manifest, manifest_dict)
        except (ValueError, KeyError) as e:
            failedArticles.append(name)
            print('Error with {0}: {1}'.format(name, e), file=sys.stderr)
    if len(failedArticles) > 0:
        print('Articles that failed to repackage.')
        print(failedArticles)

def cmd_list_dois(options):
    """
    List the DOIs associated with the prefix
    """
    articleList = fetchArticleList(options)
    for doi in articleList:
        print(doi)

def cmd_meta(options, args):
    """
    """
    for name in args:
        meta = fetchArticleMeta(name, options)
        print(pretty_dict_repr(meta))    

def cmd_assetmeta(options, args):
    """
    """
    for name in args:
        meta = fetchAssetMeta(options, afid=name)
        print(pretty_dict_repr(meta))

def cmd_assetfiles(options, args):
    """
    """
    for name in args:
        assetFiles = fetchAssetFiles(name, options)
        print(pretty_dict_repr(assetFiles))

def main(options, args):
    """
    Main entry point for parsing the commands.
    """
    failedArticles = []
    cmd = options.command
    if cmd == 'REPACKAGE':
        cmd_repackage(options, args)
    elif cmd == 'META':
        cmd_meta(options, args)
    elif cmd == 'ASSET-META':
        cmd_assetmeta(options, args)
    elif cmd == 'ASSET-FILES':
        cmd_assetfiles(options, args)
    elif cmd == 'LIST':
        cmd_list_dois(options) 

if __name__ == "__main__":
    """
    """
    usage = """
    usage: %prog [options] arg1 arg2 ...
    
    COMAND                   Description
     LIST            List the DOI's within a perfix
     REPACKAGE       Repackage the list of article names into zip packages.
     META            Dump the meta-data for the list of article names.
     ASSET-META      Dump the asset meta-data for the list of article names.
     ASSET-FILES
       
    """
    parser = OptionParser(usage=usage)
    parser.add_option('--command', action='store', type='string',
                      dest='command', default='LIST', metavar='COMMAND',
                      help='Command to execute. LIST, REPACKAGE [default: %default]')
    parser.add_option('--server', action='store', type='string',
                      dest='rhinoServer', metavar='SERVER',
                      help='rhino server url ex. "https://webprod.plosjournals.org/api"')
    parser.add_option('--prefix', action='store', type='string', dest='prefix',
                      default='10.1371', metavar='PREFIX',
                      help='prefix used in constructing article DOI. [default: %default]')
    parser.add_option('--rver', action='store', type='string', dest='rver',
                      default='v1', metavar='RVER',
                      help='Rhino api version to use. [default: %default]')
    parser.add_option('--verify', action='store_true', dest='verify',
                      default=False, metavar='VERIFY',
                      help='Require certificates for https. [default: %default]')


    (options, args) = parser.parse_args()
    sys.exit(main(options, args))
