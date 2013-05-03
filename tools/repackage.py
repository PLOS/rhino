#!/usr/bin/python2

"""
    Scrape article XMLs and package them for ingestion.

    Usage: repackage.py rhino_url prefix article_name

    Note:
        rhino_url - is the usl to the rhino SOA for the system you are interested in.
        prefix    - the doi prefix (for PLOS 10.1371)
        article_name - the first part of the article zip name.

   Ex. repackage.py --server="https://webprod.plosjournals.org"  "pone.0033205"
"""

from __future__ import print_function
from __future__ import with_statement
from cStringIO import StringIO
from optparse import OptionParser

import os, sys, string
import requests
import zipfile
import json

"""
***********************************************
     Text templates used in 
     repetative string creation.
***********************************************
"""
""" ***** Manifest DTD template *****"""
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

""" ***** Skeletal Manifest Template ***** """
MANIFEST_TMPL = """<?xml version="1.1"  encoding="UTF-8"?>
<!DOCTYPE manifest SYSTEM "manifest.dtd">
<manifest>
  <articleBundle>
    {article}
    {objects}
  </articleBundle>
</manifest>
""".lstrip()

""" ***** Manifest Object Entries *****"""
OBJECT_TMPL = """    <object uri="{uri}" {strkimage}> 
        {reps}
     </object>"""

""" ***** Object Representation Template ***** """
REPRESENTATION_TMPL = """<representation name="{name}" entry="{entry}" />"""

""" ***** Manifest Article Template *****"""
ARTICLE_TMPL = """
     <article uri="{uri}" main-entry="{fname}">
        {reps}
     </article>"""

""" ***** URI Template *****"""
URI_TMPL = """info:doi/{prefix}/journal.{name}"""

""" ***** DOI Template *****"""
DOI_TMPL = """{prefix}/journal.{name}"""

FETCH_URL_TMPL = '{server}/api/assetfiles/{doi}.{ext}'


"""*****************************************************"""
"""
    Fetch the information necessary to build a manifest from 
    Rhino.
"""
def fetchManifestInfo(name, options):
        (base_url, prefix) = (options.rhinoServer, options.prefix)
        doi = DOI_TMPL.format(prefix=prefix, name=name)
	uri = URI_TMPL.format(prefix=prefix, name=name)
	url = base_url + '/api/articles/{doi}'.format(doi=doi)
	response = requests.get(url, verify=False)	
	
	"""Load JSON into a Python object and use some values from it."""
	article = json.loads(response.content)
	assets = {}
        strkImageURI = ''
        if 'strkImageURI' in article:
                strkImageURI = article['strkImgURI'] 
	for asset in article['assets']:
                ext = asset['extension']
                asset_name = string.joinfields(asset['doi'].split(".")[2:], '.')
		""" Technically the pdf and xml article file is not an object """
		if asset_name == name:
			continue
		if asset_name in assets:
			assets[asset_name].append(ext)
		else:
			assets[asset_name] = [ext]
	return { 'URI' : uri,
	         'doi' : doi,
	         'prefix' : prefix,
	         'xml_file' : '{name}.{ext}'.format(name=name, ext='xml'),
	         'pdf_file' : '{name}.{ext}'.format(name=name, ext='pdf'),
	         'strkImageURI' : strkImageURI,
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

"""
    Most of the files other than the article xml is 
    binary in nature. Fetch the data and write it to
    a temporary file.
"""
def fetchBinary(filename, url):
	r = requests.get(url, verify=False)
	if r.status_code == 200:
		with open(filename, 'wb') as f:
			for chunk in r.iter_content(1024):
				f.write(chunk)
	else:
		print("not downloaded  " + url)
"""
    Take the information we have gather about the article so far
    and create the zip.
"""
def make_zip(rhinoServer, name, manifest, md, assetTupleList):
    fetch = requests.get(FETCH_URL_TMPL.format(server=rhinoServer, doi=md['doi'], ext='xml'), verify=False)
    zip_path = os.path.join('.', name + '.zip')
    with zipfile.ZipFile(zip_path, mode='w') as zf:
            zf.writestr('MANIFEST.xml', manifest, compress_type=zipfile.ZIP_DEFLATED)
	    zf.writestr(md['xml_file'], fetch.content, compress_type=zipfile.ZIP_DEFLATED)
	    zf.writestr('manifest.dtd', DTD_TEXT, compress_type=zipfile.ZIP_DEFLATED)
	    url = FETCH_URL_TMPL.format(server=rhinoServer, doi=md['doi'], ext='pdf')
	    fetchBinary(md['pdf_file'], url)
	    zf.write(md['pdf_file'], md['pdf_file'] , compress_type=zipfile.ZIP_DEFLATED)
	    os.remove(md['pdf_file'])
	    for t in assetTupleList:
		    (assetName, assetDOI, ext) = t
		    print(assetDOI)
		    url = FETCH_URL_TMPL.format(server=rhinoServer, doi=assetDOI, ext=ext.lower())
		    name = '{name}.{ext}'.format(name=assetName, ext=ext.lower()) 
		    fetchBinary(name, url)
		    zf.write(name, name , compress_type=zipfile.ZIP_DEFLATED)
		    os.remove(name)
"""
   Build a set of representation entities and tuples
   with the name, uri and extension of the the asset.
"""
def buildReps(name, reps, prefix='10.1371'):
	repsTags = []
	nfeTuples = []
	for ext in reps:
		fn = '{name}.{ext}'.format(name=name, ext=ext.lower())
		nfeTuples.append((name, DOI_TMPL.format(prefix=prefix, name=name), ext))
		repsTags.append(REPRESENTATION_TMPL.format(name=ext,entry=fn))
	return (string.joinfields(repsTags, "\n       "), nfeTuples)
"""
   Build a set of object entities and collect a list
   of tuples containing the name, uri and extension of
   the the respresentaions of that object.
"""	
def buildObjectTags(md):
	objects = []
	objectTuples = []
	strkImage = md['strkImageURI']
	for (k, v) in md['assets'].items():
		asset_uri =  URI_TMPL.format(prefix=md['prefix'], name=k)
		strkValue = ''
		if strkImage == asset_uri : strkValue = 'strkImage="True"' 
		(reps, nueTuples) = buildReps(k, v)
		objectTuples += nueTuples
		objects.append(OBJECT_TMPL.format(uri=asset_uri, strkimage=strkValue,reps=reps))
	return (string.joinfields(objects, '\n'), objectTuples)
	
"""
   Build a Article entity with the the XML and PDF representation
   tags included.
"""
def buildArticleTag(md):
	xmlRep = REPRESENTATION_TMPL.format(name='XML', entry=md['xml_file']) 
	pdfRep = REPRESENTATION_TMPL.format(name='PDF', entry=md['pdf_file']) 
	return ARTICLE_TMPL.format(uri=md['URI'], fname=md['xml_file'], reps=xmlRep + "\n        " + pdfRep)

"""
   Build the manifest for the specified article.
"""	
def build_manifest_xml(md):
	articleTag = buildArticleTag(md)
	(objectTags, nueTuples) = buildObjectTags(md)
	return (MANIFEST_TMPL.format(article=articleTag, objects=objectTags), nueTuples)				
	
def main(options, args):
        failedArticles = []
	for name in args:
               try:
                       print('Fetch manifest')
                       manifest_dict = fetchManifestInfo(name=name, options=options)
                       print('Build manifest')
		       (manifest, assetTupleList) = build_manifest_xml(manifest_dict)
                       print('Zip it up {s}  {n}'.format(s=options.rhinoServer, n=name))
		       make_zip(options.rhinoServer, name, manifest, manifest_dict, assetTupleList)
               except (ValueError, KeyError) as e:
                       failedArticles.append(name)
                       sys.stderr.write('Error with  {0}: {1}'.format(name, e))
        if len(failedArticles) > 0:
               print('Articles that failed to repackage.')
               print(failedArticles) 

if __name__ == "__main__":

        usage = "usage: %prog [options] arg1 arg2 ..."
        parser = OptionParser(usage=usage)
        parser.add_option('--server', action='store', type='string', 
                          dest='rhinoServer', metavar='SERVER', 
                          help='rhino server url ex. "https://webprod.plosjournals.org"')
        parser.add_option('--prefix', action='store', type='string', dest='prefix', 
                          default='10.1371', metavar='PREFIX', 
                          help='prefix used in constructing article DOI.i [default: %default]')
          
        (options, args) = parser.parse_args()
        if len(args) < 1:
                parser.error('incorrect number of arguments.')
                sys.exit(1)
	sys.exit(main(options, args))
