#!/usr/bin/env python2

__author__ = 'fcabrales@plos.org'

"""
This test case validates Rhino's ingestibles API.
This test requires sshpass to be installed %apt-get install sshpass
"""

import os, random, subprocess
from ..api.RequestObject.ingestibles_json import IngestiblesJson, OK, CREATED, NOT_ALLOWED
from ..Base.Config import INGESTION_HOST, INGEST_USER, RHINO_INGEST_PATH, SSH_PASSWORD
import resources

INGESTED_DATA_PATH = '/var/spool/ambra/ingested'
INGESTION_QUEUE_DATA_PATH = '/var/spool/ambra/ingestion-queue'
HOST_HOME = INGEST_USER +'@'+ INGESTION_HOST
USER_HOME = '/home/' + INGEST_USER + '/'

class IngestiblesTest(IngestiblesJson):

  def setUp(self):
    pass

  def tearDown(self):
    pass

  def test_get_ingestibles(self):
    """
    Get should return the files from ingest directory.
    """
    files = self.copy_file_to_ingest(count=2)
    self.get_ingestibles()
    self.verify_http_code_is(OK)
    self.verify_get_ingestibles(names=files)
    self.delete_files_in_ingest(files)

  def test_post_ingestibles(self):
    """
    Ingest with force_reingest should succeed.
    """
    files = self.copy_file_to_ingest(count=1)
    self.assertEquals(len(files), 1, 'cannot find any ingestible file')
    try:
      self.post_ingestibles(name=files[0], force_reingest='')
      self.verify_http_code_is(CREATED)
      self.verify_zip_ingestion(files[0])
    except:
      # delete file if there was exception, otherwise Rhino already moves it
      self.verify_ingest_files(exists=files)
      self.delete_files_in_ingest(files)
      raise
    self.verify_ingest_files(missing=files)

  def test_post_ingestibles_noforce(self):
    """
    Second ingest without force_reingest should fail.
    """
    files = self.copy_file_to_ingest(count=1)
    self.assertEquals(len(files), 1, 'cannot find any ingestible file')
    try:
      self.post_ingestibles(name=files[0], force_reingest='')
      self.verify_http_code_is(CREATED)
      self.verify_zip_ingestion(files[0])
    except:
      self.delete_files_in_ingest(files)
      raise
    self.copy_file_to_ingest()
    try:
      # TODO: response here is not JSON, is that a bug?
      # So I added a param to not parse as json.
      self.post_ingestibles(name=files[0], parse=False)
      self.verify_http_code_is(NOT_ALLOWED)
      self.delete_files_in_ingest(files)
    except:
      self.verify_ingest_files(exists=files)
      self.delete_files_in_ingest(files)
      raise

  # copy N files from INGESTED_DATA_PATH variable directory to Rhino's ingest directory
  def copy_file_to_ingest(self, count=1):
    files = []
    try:
      COMMAND= 'sshpass -p' + SSH_PASSWORD + ' ssh -o StrictHostKeyChecking=no ' + HOST_HOME + ' ls ' + INGESTED_DATA_PATH
      string_file_names = subprocess.check_output(COMMAND, shell=True)
      counter = string_file_names.count('.zip')
      random_number = random.randint(0,counter-1)
      record = string_file_names.split('\n')[random_number]
      files.append(record)
    except:
      raise RuntimeError('error reading directory %r'%(INGESTED_DATA_PATH,))
    random.shuffle(files)
    files = files[:count]
    for filename in files:
      print(filename)
      COMMAND= 'sshpass -p' + SSH_PASSWORD + ' ssh -o StrictHostKeyChecking=no ' + HOST_HOME + ' sudo cp ' + INGESTED_DATA_PATH + '/' + filename + ' ' +  INGESTION_QUEUE_DATA_PATH
      string_files_moved = subprocess.check_output(COMMAND, shell=True)
      print (string_files_moved)
    return files

  def delete_files_in_ingest(self, files):
    for filename in files:
      src = os.path.join(RHINO_INGEST_PATH, filename)
      try:
        COMMAND_DELETE = ' sudo rm ' + src
        COMMAND= 'sshpass -p' + SSH_PASSWORD + ' ssh -o StrictHostKeyChecking=no ' + HOST_HOME + COMMAND_DELETE
        print('sshpass -pPassword ssh -o StrictHostKeyChecking=no ' + HOST_HOME + COMMAND_DELETE)
        os.system(COMMAND)
      except:
        raise RuntimeError('error removing from %r'%(src,))

  def verify_ingest_files(self, exists=None, missing=None):
    if exists:
      for filename in exists:
        src = os.path.join(RHINO_INGEST_PATH, filename)
        self.assertTrue(os.path.exists(src), 'file is missing in ingest directory: %r'%(src,))
    if missing:
      for filename in missing:
        src = os.path.join(RHINO_INGEST_PATH, filename)
        self.assertFalse(os.path.exists(src), 'file exists in ingest directory: %r'%(src,))

  def verify_zip_ingestion(self, files):
    # Validate response with Syndication table
    self.verify_syndications(files)
    # Validate response with Journal table
    self.verify_journals(files)
    # Validate response with CitedArticle and CitedPerson tables
    self.verify_citedArticles(files)
    # Validate response with ArticleAsset table
    self.verify_article_file(files, resources.PDF_CONTENT_TYPE, 'articlePdf')
    self.verify_article_file(files, resources.XML_CONTENT_TYPE, 'articleXml')
    # self.verify_article_figures(files)
    self.verify_article_graphics(files)

if __name__ == '__main__':
  IngestiblesTest._run_tests_randomly()