#!/usr/bin/env python
"""
  Description: The new repository that will support versions
  of assets will need to have the old corpus migrated to it.
  This script is designed to help do that primarily by creating 
  bash scripts and files that inset data into mysql or upload
  files to MogileFS named as their sha1 hash.
"""

from __future__ import print_function
from __future__ import with_statement
import sys, string, urllib
from plosapi import Rhino

_DB_PROLOG = '''
USE {dbname};

CREATE TABLE IF NOT EXISTS buckets (
  bucketId INTEGER NOT NULL AUTO_INCREMENT,
  bucketName VARCHAR(255) NOT NULL,
  UNIQUE KEY keyBucket (bucketName),
  PRIMARY KEY (bucketId)
);

CREATE TABLE IF NOT EXISTS objects (
  id INTEGER NOT NULL AUTO_INCREMENT,
  bucketId INTEGER REFERENCES buckets,
  objkey VARCHAR (255) NOT NULL,
  checksum VARCHAR (255) NOT NULL,
  timestamp timestamp NOT NULL,
  urls VARCHAR (3000),
  downloadName VARCHAR (1000),
  contentType VARCHAR (200),
  size INTEGER NOT NULL,
  tag VARCHAR (200),
  status TINYINT DEFAULT 0 NOT NULL,
  versionNumber INTEGER NOT NULL,
  UNIQUE KEY keySum (bucketId, objkey, versionNumber),
  PRIMARY KEY (id)
);

'''

_INSERT_STR = "INSERT INTO objects (bucketId, objkey, checksum, timestamp, downloadName, contentType, size, tag, status, versionNumber) " \
              "VALUES ({bkt}, '{objkey}', '{cs}', '{ts}', '{dn}', '{ct}', {sz}, '{tag}', 0, {ver});"
def decode_row(r):
  """
  """
  (doi, tstamp, afid_tmp, md5, sha1, ct, sz, _) = [string.strip(e) for e in r.split(',')]
  ts = string.replace(tstamp, 'T', ' ')
  ts = string.replace(ts, 'Z', '')
  afid = urllib.unquote(afid_tmp)
  dname = urllib.quote(doi, '')
  return (doi, ts, afid, md5, sha1, ct, sz, dname, afid_tmp)

def mk_inserts(infile, args):
  """
  """
  print(_DB_PROLOG.format(dbname=args.dbname))
  for row in infile:
    (doi, ts, afid, md5, sha1, ct, sz, _, _) = decode_row(row)
    objKey = args.doiPrefix + afid
    insert_string = _INSERT_STR.format(bkt=args.bucketID, objkey=objKey, cs=sha1, ts=ts, dn=afid, ct=ct, sz=sz, tag=args.tag, ver=args.ver)
    print(insert_string)
  return

def mogupload(infile, args):
  """
  """
  mog_str = "mogupload --domain={d} --key='{k}' --file='{up}{dname}/{fname}' ; echo 'Processed: {fname}'"
  for row in infile:
    (doi, ts, afid, md5, sha1, ct, sz, dname, fname) = decode_row(row)
    print(mog_str.format(up=args.basedir, d=args.mogdomain, k=sha1, dname=dname, fname=fname))
  return

def moginfo(infile, args):
  """
  """
  mog_str = "echo -n '{k} ' ; mogfileinfo --domain={d} --key='{k}' | grep http | tr '\\n' ' '; echo ''"
  for row in infile:
    (doi, ts, afid, md5, sha1, ct, sz, dname, fname) = decode_row(row)
    print(mog_str.format(key=sha1,d=args.mogdomain, k=sha1))
  return

def diff_new(infile, args):
  """
  """
  old = dict()
  for row in infile:
    (doi, ts, afid, md5, sha1, ct, sz, dname, fname) = decode_row(row)
    old['10.1371/'+doi] = ts
  rhino = Rhino()
  current = dict()
  for (doi, mod_date) in rhino.articles(lastModified=True):
    mod_date = mod_date.replace('T', ' ')
    mod_date = mod_date.replace('Z', '')
    current[doi] = mod_date
    
  for (doi, mod_date) in current.iteritems():
      if not old.has_key(doi):
         print(doi.replace('10.1371/', ''))
  return

def diff_mod(infile, args):
  """
  """
  old = dict()
  for row in infile:
    (doi, ts, afid, md5, sha1, ct, sz, dname, fname) = decode_row(row)
    old['10.1371/'+doi] = ts
  rhino = Rhino()
  current = dict()
  for (doi, mod_date) in rhino.articles(lastModified=True):
    mod_date = mod_date.replace('T', ' ')
    mod_date = mod_date.replace('Z', '')
    current[doi] = mod_date

  for (doi, mod_date) in old.iteritems():
    if not current.has_key(doi):
      continue
      #print(doi + ' missing')
    elif not current[doi] == mod_date:
      print(doi.replace('10.1371/', ''))

  return

def mk_filestore(infile, args):
  """
  """
  mkdir_str = 'mkdir {up}{dir} #make directory'
  mkln_str = 'ln --force {src} {dst} #link files'

  dir_map = dict()
  for row in infile:
    (doi, ts, afid, md5, sha1, ct, sz, dname, fname) = decode_row(row)
    k = sha1[:2]
    if not dir_map.has_key(k):
      dir_map[k] = []
    dir_map[k].append(('{up}{d}/{f}'.format(up=args.baseout, d=k, f=sha1), '{up}{d}/{f}'.format(up=args.basedir, d=dname, f=fname)))

  for k in dir_map.keys():
    print(mkdir_str.format(up=args.baseout, dir=k))

  for k,v in dir_map.iteritems():
    for pair in v:
        print(mkln_str.format(src=pair[1], dst=pair[0]))

  return 

if __name__ == '__main__':
  import argparse

  parser = argparse.ArgumentParser(description='Output to stdout a set of MySQL insert statements representing a PLOS corpus in Repository schema. '
                                               'It is assumed the database already exist in MySQL.')
  parser.add_argument('--file', help='A csv with asset file meta-data.')
  parser.add_argument('--dbname', default='PLOS_REPO', help='The name of the repository database to use.')
  parser.add_argument('--tag', default='Initial Version', help='The tag associated with assets being imported.')
  parser.add_argument('--bucketID', default='1',  help='Bucket ID to use for imports.')
  parser.add_argument('--ver', default='1', help='The version to assign to the imported afid')
  parser.add_argument('--doiPrefix', default='10.1371/', help='Prefix to add to the object key name.')
  parser.add_argument('--mogdomain', default='plos_repo', help='MogileFS domain to upload to.')
  parser.add_argument('--basedir', default='', help='Base directory to include in script file paths.')
  parser.add_argument('--baseout', default='', help='Base directory to use for output in some scripts.')
  parser.add_argument('command', help='Commands: db | moginfo | mogupload | diff | filestore') 
  #parser.add_argument('params', nargs='*', help="parameter list for commands")
  args = parser.parse_args()
  #params = args.params

  infile = sys.stdin
  if args.file:
    infile = open(args.file, 'r')
 
  if args.command == 'db':
    mk_inserts(infile, args)
    sys.exit(0)

  if args.command == 'mogupload':
    mogupload(infile, args)
    sys.exit(0)

  if args.command == 'moginfo':
    moginfo(infile, args)
    sys.exit(0)

  if args.command == 'diffnew':
    diff_new(infile, args)
    sys.exit(0)

  if args.command == 'diffmod':
    diff_mod(infile, args)
    sys.exit(0)

  if args.command == 'filestore':
    mk_filestore(infile, args)
    sys.exit(0)
   
