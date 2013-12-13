#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
  Generate correction ingestibles

  Accepts a source directory location and a destination directory location

  This script:
  * Looks at every file (XML) in the source directory given
  * Runs a XSL Transform on the XML to make it PLOS friendly
  * Dowloads from the plos website any associated resources
  * Tries to correct a bunch of known errors in XML and asset names
  * Creates a manifest file
  * Creates a zip archive with the XML and writes this to disk for each article XML
"""

# Attention.  There are a number of places where I 'fudge' the data to get around
# errors.  Everyplace I do that in the code below has a "#FIX_DATA" text next to it

import sys, zipfile, shutil, os, httplib2, json, urllib2, csv, tarfile, MySQLdb, re
from urlparse import urlparse
from lxml import etree
from os import listdir
from os.path import isfile, join, splitext, basename
from collections import OrderedDict
from ftplib import FTP

#Working folder for building up zip files
tempFolder = "correctionTemp"
cacheFolder = "create_corrections_cache"

xlinkNamespace = "{http://www.w3.org/1999/xlink}"
#define these globally, they'll never change
removeNSXslt = etree.XML(open('xsl/removeNamespace.xsl', 'r').read())
removeNSTransform = etree.XSLT(removeNSXslt)
articleNodeXslt = etree.XML(open('xsl/selectArticleNode.xsl', 'r').read())
articleNodeTransform = etree.XSLT(articleNodeXslt)
plosifyArticleXslt = etree.XML(open('xsl/plosifyArticle.xsl', 'r').read())
plosifyArticleTransform = etree.XSLT(plosifyArticleXslt)

def createIngestableForArticle(file, destination):
  articleXML = open(file, 'r').read()
  articleDOM = etree.XML(articleXML)
  articleDOM = plosifyArticle(articleDOM)

  correctionPMCID = getCorrectionPMCID(articleDOM)
  correctionDOI = getCorrectionDOI(articleDOM)
  correctedArticleDOI = None

  #A few edge cases where the related article node has no DOI or PMC ID
  #FIX_DATA
  if(correctionDOI == "10.1371/annotation/5fbbf39a-fb47-4ce1-8069-acd830b3d41f"):
    correctedArticleDOI = "10.1371/journal.pone.0002432"

  if(correctionDOI == "10.1371/annotation/b8b66a84-4919-4a3e-ba3e-bb11f3853755"):
    correctedArticleDOI = "10.1371/journal.pone.0002432"

  if(correctionDOI == "10.1371/annotation/897f96fc-db2e-4ee5-b77b-4ad95768da47"):
    correctedArticleDOI = "10.1371/journal.pbio.0060327"

  if(correctionDOI == "10.1371/annotation/363b6074-caec-4238-b88f-acbf45de498f"):
    correctedArticleDOI = "10.1371/journal.pcbi.0030099"

  if(correctionDOI == "10.1371/annotation/2259f958-a68e-4e57-92b5-2ef003070cf1"):
    correctedArticleDOI = "10.1371/journal.pbio.1000099"

  if(correctedArticleDOI == None):
    correctedArticleDOI = getCorrectedArticleDOI(articleDOM)

  if(correctedArticleDOI == None):
    raise Exception("Can not find corrected article DOI")

  #Some really bad data here.  The annotations are flat out wrong?  HUH?  
  #FIX_DATA
  if(correctionDOI == "10.1371/annotation/8f2ddf91-3499-4627-9a91-449b78465f9d"):
    correctionDOI = "10.1371/annotation/1345f9b0-016e-4123-9e72-6ccc4fa17ba2"

  if(correctionDOI == "10.1371/annotation/ccd51e37-fdf3-466e-b79f-2c390df9ab28"):
    correctionDOI = "10.1371/annotation/5d2e13d4-cb4e-44b1-a836-dff9b4144b5b"

  if(correctionDOI == "10.1371/annotation/80bc1e50-d623-464f-817f-a5e776b75717"):
    correctionDOI = "10.1371/annotation/2505c54a-329b-4553-ad82-cb4b38100ffa"

  if(correctionDOI == "10.1371/annotation/33d82b59-59a3-4412-9853-e78e49af76b9"):
    correctionDOI = "10.1371/annotation/deacc2fd-665b-4736-b668-dc69a38bb4f9"

  if(correctionDOI == "10.1371/annotation/5e4082fd-6d86-441f-b946-a6e87a22ea57"):
    correctionDOI = "10.1371/annotation/d9496d01-8c5d-4d24-8287-94449ada5064"

  if(correctionDOI == "10.1371/annotation/a40342be-8f9e-4650-a845-1e91ff06c27c"):
    correctionDOI = "10.1371/annotation/56ccf999-c461-4598-a883-55fbac9751a6"

  #Fix another inconsistancy, some DOIs have info:doi/, some not.
  #FIX_DATA
  correctedArticleDOI = correctedArticleDOI.replace("info:doi/","")
  annotationBody = getAnnotationBodyFromDB(correctionDOI)

  assetURLs = getAssetsFromAnnotationBody(annotationBody)
  assetDict = fetchCorrectionAssets(assetURLs)

  articleDOM = replaceCorrectionBody(assetDict, annotationBody, articleDOM)

  #Create Manifest and write to temp folder
  #manifestFile = createManifest(correctionDOI, assetDict)
  manifestFile = None
  xmlFile = "{0}/{1}.xml".format(tempFolder, 
    correctionDOI.replace("10.1371/annotation/",""))
  
  #Write XML to disk
  #Kludgy way of appending XML DOC Type data.  TODO: Improve on this?
  writeFile(xmlFile,  """<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE article
    PUBLIC "-//NLM//DTD Journal Publishing DTD v3.0 20080202//EN" "http://dtd.nlm.nih.gov/publishing/3.0/journalpublishing3.dtd">
  """ + etree.tostring(articleDOM))

  writeZipFile(correctionDOI, xmlFile, manifestFile, assetDict, destination)
  
  #TODO: cleanup

#Create zip and place in destination
def writeZipFile(correctionDOI, xmlFile, manifestFile, assetDict, destination):
  zipFileName = "{0}/{1}.zip".format(destination, correctionDOI.replace("10.1371/annotation/",""))

  zipFile = zipfile.ZipFile(zipFileName, "w")
  zipFile.write(xmlFile, os.path.basename(xmlFile), zipfile.ZIP_DEFLATED)
  #zipFile.write(manifestFile, os.path.basename(manifestFile), zipfile.ZIP_DEFLATED)

  for assetDOI, assetData in assetDict.iteritems():
    zipFile.write(assetData["filePathName"], assetData["filename"], zipfile.ZIP_DEFLATED)

  zipFile.close()

  return

#Fetch correction assets, build up a dictionary of the results
#Cache the binary data
def fetchCorrectionAssets(assetURLs):
  results = dict()

  for assetURL in assetURLs:
    filename = basename(urlparse(assetURL).path)

    #A lot of our assets have inconsitant names.  Here I attempt to bring
    #them in line
    #FIX_DATA
    if(filename == "pgen.1000235.cn.g006.tif"):
      filename = "pgen.1000235.g006.cn.tif"

    if(filename == "pbio.1000056.cn.sd001.doc"):
      filename = "pbio.1000056.sd001.cn.doc"

    if(filename == "pbio.1000056.cn.sd005.doc"):
      filename = "pbio.1000056.sd005.cn.doc"

    if(filename == "pgen.1001247.s001.pdf"):
      filename = "pgen.1001247.s001.cn.pdf"

    if(filename == "pmed.1000362.g001.tif"):
      filename = "pmed.1000362.g001.cn.tif"

    if(filename == "pone.0051833.001.cn.docx"):
      filename = "pone.0051833.s001.cn.docx"

    if(filename == "pone.0024024.001.cn.txt"):
      filename = "pone.0024024.s001.cn.txt"

    if(filename == "pone.0024024.002.cn.doc"):
      filename = "pone.0024024.s002.cn.doc"

    if(filename == "pbio.1000102.cn.sd005.doc"):
      filename = "pbio.1000102.sd005.cn.doc"

    if(filename == "pbio.0060178.cn.g002.tif"):
      filename = "pbio.0060178.g002.cn.tif"

    ##TODO: More cleanup

    assetFilePathName = "{0}/{1}".format(cacheFolder, filename)
    
    print assetFilePathName
    
    if not os.path.exists(assetFilePathName):
      resp, content = httplib2.Http().request(assetURL)
      with open(assetFilePathName, "wb") as f:
        f.write(content)

    assetDOI, extension = functionCreateAssetDOI(assetFilePathName)
    results[assetDOI] = { "url": assetURL, "filename": filename,
      "filePathName": assetFilePathName, 
      "extension": extension }

  return results

def functionCreateAssetDOI(assetFilePathName):
  #Given: pone.0040621.g002.cn.tif return pone.0040621.g002.cn
  path, assetDOI = os.path.split(assetFilePathName)
  assetDOI, extension = os.path.splitext(assetDOI)

  #splitext leaves the period attached
  extension = extension.lstrip(".")
  assetDOI = "10.1371/journal.{0}".format(assetDOI)

  print "assetDOI: {0}, Extension: {1}".format(assetDOI, extension)
  return assetDOI, extension

def replaceCorrectionBody(assetDict, annotationBody, articleDOM):
  #Fix invalid XML characters
  annotationBody = annotationBody.replace("&","&amp;").replace('>', '&gt;').replace('<', '&lt;')

  for assetDOI, assetData in assetDict.iteritems():
    #print assetDOI
    assetType = assetDOI.split(".")[4]

    #print annotationBody

    print "assetType: {0}".format(assetType)

    #Handle new PDF?
    if(assetType == "cn" or "s" in assetType):
      annotationBody = annotationBody.replace(assetData["url"], """
          <supplementary-material content-type="local-data">
            <media xlink:href="info:doi/{0}" mimetype="application" mime-subtype="{1}" xlink:type=\"simple\">
              <caption></caption>
            </media>
          </supplementary-material>
        """.format(assetDOI, assetData["extension"]))

    if("g" in assetType):
      print assetData["url"]
      annotationBody = annotationBody.replace(assetData["url"], "<fig><graphic xlink:href=\"info:doi/{0}\" xlink:type=\"simple\"/></fig>".format(assetDOI))
      
  #print annotationBody

  #There should only ever be one body, this is just easy to write / read
  #article
  annotationBody = "<p xmlns:xlink=\"http://www.w3.org/1999/xlink\">{0}</p>".format(annotationBody.replace("&","&amp;"))
  #print annotationBody
  articleDOM.replace(articleDOM.find("body"), etree.fromstring(annotationBody))
  
  #TODO: Handle e0
  #TODO: Handle t0
    
  return articleDOM

def getAssetsFromAnnotationBody(annotationBody):
  #http://stackoverflow.com/questions/6883049/regex-to-find-urls-in-string-in-python
  urls = re.findall('http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\(\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+', annotationBody)
  results = set()
  
  for url in urls:
    #Hackish way of only including corrections URLs (I didn't want to spent time modifying the crazy regex)
    if("corrections" in url):
      #Sometimes the regex allows trailing periods
      url = url.rstrip(".)")
      results.add(url)

  return results

def getAnnotationBodyFromDB(correctionDOI):
  cur = con.cursor()
  sql = "select body from annotation where annotationURI = 'info:doi/{0}'".format(correctionDOI)

  #print (sql)
  cur.execute(sql)

  rows = cur.fetchall()
  for row in rows:
    #FIX_DATA
    return row[0].replace("http://plos", "http://www.plos")

  raise Exception("Cound not find annotation for {0}".format(correctionDOI))

#Get the corrected article DOI from the XML
#The XML comes in many flavors:
#related-article ext-link-type="doi" xlink:href="10.1371/journal.pone.0038862"
#<related-article xlink:href="info:doi/10.1371/journal.pone.0034777" related-article-type="corrected-article" ext-link-type="uri">
#<related-article id="d35e81" related-article-type="corrected-article" ext-link-type="doi" xlink:href="10.1371/journal.pone.0074463" vol="8" page="e74463">
#related-article ext-link-type="doi"
#related-article xlink:href="info:doi/10.1371/journal.pone.0035155"
def getCorrectedArticleDOI(articleDOM):
  #Yeah, could make this a lot safer
  nodeList = articleDOM.findall(".//related-article[@ext-link-type='uri']")

  if(len(nodeList) > 0):
    return nodeList[0].get("{0}href".format(xlinkNamespace))

  nodeList = articleDOM.findall(".//related-article[@ext-link-type='doi']")

  if(len(nodeList) > 0):
    return nodeList[0].get("{0}href".format(xlinkNamespace))

  #No DOI found, now let's try pubmedID
  pmID = None
  nodeList = articleDOM.findall(".//related-article[@ext-link-type='pubmed']")

  if(len(nodeList) > 0):
    pmID = nodeList[0].get("{0}href".format(xlinkNamespace))
  else:
    nodeList = articleDOM.findall(".//related-article[@ext-link-type='pmc']")
    
    if(len(nodeList) > 0):
      pmID = nodeList[0].get("{0}href".format(xlinkNamespace))
    else:
      raise Exception("Can not identify corrected article DOI.  Check the XML / xpath statements")

  if(pmID == None):
    raise Exception("Can not identify corrected article DOI.  Check the XML / xpath statements")

  #Got pubmedID, lets turn it into a DOI
  return getPMID2Doi(pmID)
  
  #return articleDOM.findall(".//related-article[@ext-link-type='doi']")[0].get("{0}:href".format(xlinkNamespace))

def readFile(path):
  content = None

  if(os.path.exists(path)):
    f = open(path, "r")
    try:
      # Read the entire contents of a file at once.
      content = f.read() 
    finally:
      f.close()

  return content

def writeFile(path, contents):
  f = open(path, "w")

  try:
    f.write(contents)
  finally:
    f.close()

#Get the DOI for a given PMID
def getPMID2Doi(pmcRefID):
  filename = "{0}/doi-{1}.json".format(cacheFolder,pmcRefID)

  doi = readFile(filename)

  if(doi == None):
    url = "http://www.pmid2doi.org/rest/json/doi/{0}".format(pmcRefID)
    resp, content = httplib2.Http().request(url)
    jsonResponse = json.loads(content)
    doi = jsonResponse["doi"]
    writeFile(filename, doi)

  if(doi == None):
    raise Exception("Can not identify DOI. :-( ")

  return doi

#XPATH helper functions
def getCorrectionPMCID(articleDOM):
  #Yeah, could make this a lot safer
  return articleDOM.findall(".//article-id[@pub-id-type='pmc']")[0].text

def getCorrectionDOI(articleDOM):
  #Yeah, could make this a lot safer
  return articleDOM.findall(".//article-id[@pub-id-type='doi']")[0].text

# <?xml version="1.0" encoding="UTF-8"?>
# <!DOCTYPE manifest SYSTEM "manifest.dtd">
# <manifest>
#   <articleBundle>
#     <article uri="info:doi/10.1371/annotation/0bac4872-2fa2-416e-ac45-4b0ac79f8ddd" main-entry="0bac4872-2fa2-416e-ac45-4b0ac79f8ddd.xml">
#       <representation name="XML" entry="0bac4872-2fa2-416e-ac45-4b0ac79f8ddd.xml" />
#     </article>
#     <!-- http://www.ncbi.nlm.nih.gov/pmc/articles/PMC3040641/bin/pgen.0bac4872-2fa2-416e-ac45-4b0ac79f8ddd.s001.xls -->
#     <object uri="info:doi/10.1371/annotation/0bac4872-2fa2-416e-ac45-4b0ac79f8ddd-s001">
#       <representation name="XLS" entry="0bac4872-2fa2-416e-ac45-4b0ac79f8ddd.s001.xls" />
#     </object>
#   </articleBundle>
# </manifest>

def createManifest(correctionDOI, assetDict):
  objectInfo = ""
  for assetDOI, assetData in assetDict.iteritems():
    objectInfo = objectInfo + """
      <object uri="info:doi/{0}">
        <representation name="{1}" entry="{2}" />
      </object>
    """.format(assetDOI, assetData["extension"].upper(), assetData["filename"])

  articleInfo = """
    <article uri="info:doi/{0}" main-entry="{1}.xml">
      <representation name="XML" entry="{1}.xml" />
    </article>
  """.format(correctionDOI, correctionDOI.replace("10.1371/annotation/",""))

  completeXML = """<?xml version="1.0" encoding="UTF-8"?>
  <!DOCTYPE manifest SYSTEM "manifest.dtd">
  <manifest>
    <articleBundle>{0}{1}</articleBundle>
  </manifest>
  """.format(articleInfo, objectInfo)

  manifestFile = "{0}/manifest.xml".format(tempFolder)
  writeFile(manifestFile, completeXML)

  return manifestFile


def fetchGraphics(articleDOM):
  assets = set()

  #graphic xlink:href
  #media xlink:href
  for node in articleDOM.findall(".//graphic"):
    asset = node.get("{0}href".format(xlinkNamespace))
    #print "Graphic ref:{0}".format(asset)
    assets.add(asset)

  return assets

def fetchMedia(articleDOM):
  assets = set()

  for node in articleDOM.findall(".//media"):
    asset = node.get("{0}href".format(xlinkNamespace))
    #print "Media ref:{0}".format(asset)
    assets.add(asset)

  return assets

def plosifyArticle(articleDOM):
  articleDOM = articleNodeTransform(articleDOM)
  articleDOM = removeNSTransform(articleDOM)
  articleDOM = plosifyArticleTransform(articleDOM)
  
  #TODO: Better way to do this?
  return etree.fromstring(str(articleDOM))


if __name__ == "__main__":
  import argparse   

  parser = argparse.ArgumentParser(description='Corrections ingestible generator')
  parser.add_argument('-source', '-s', required=True, help='Source directory to use')
  parser.add_argument('-destination', '-d', required=True, help='Destination directory to place zip archives')
  parser.add_argument('-dbUser', default='root', help='mysql user')
  parser.add_argument('-dbPass', default='', help='mysql password')
  parser.add_argument('-dbName', default='ambra_prod', help='mysql database')
  parser.add_argument('-dbHost', default='localhost', help='mysql host')
  parser.add_argument('-dbPort', type=int, default='3306', help='port to use with msyql host')

  args = parser.parse_args()

  con = MySQLdb.connect(host=args.dbHost, user=args.dbUser, passwd=args.dbPass, db=args.dbName, charset='utf8', use_unicode=True)

  if not os.path.exists(tempFolder):
    os.makedirs(tempFolder)

  if not os.path.exists(cacheFolder):
    os.makedirs(cacheFolder)

  for filename in listdir(args.source):
    filename = args.source + "/" + filename
    print filename
    createIngestableForArticle(filename, args.destination)
  
  con.close()

  sys.exit(0)
