#!/usr/bin/env python2

__author__ = 'msingh@plos.org'


"""
This test case validates Rhino's ingestibles API.
"""

import os, random, shutil
from ..api.RequestObject.ingestibles_json import IngestiblesJson, OK, CREATED, BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, NOT_ALLOWED

#RHINO_INGEST_PATH = "../datastores/ingest"
RHINO_INGEST_PATH = "/var/spool/ambra/ingestion-queue"
TEST_DATA_PATH = "test/data"

class IngestiblesTest(IngestiblesJson):

  def setUp(self):
    pass

  def tearDown(self):
    pass

  def test_get_ingestibles(self):
    """
    Get should return the files from ingest directory.
    """
    files = self.copy_files_to_ingest(count=2)
    self.get_ingestibles()
    self.verify_http_code_is(OK)
    self.verify_get_ingestibles(names=files)
    self.delete_files_in_ingest(files)

  def test_post_ingestibles(self):
    """
    Ingest with force_reingest should succeed.
    """
    files = self.copy_files_to_ingest(count=1)
    self.assertEquals(len(files), 1, "cannot find any ingestible file");
    try:
      self.post_ingestibles(name=files[0], force_reingest='')
      self.verify_http_code_is(CREATED)
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
    files = self.copy_files_to_ingest(count=1)
    self.assertEquals(len(files), 1, "cannot find any ingestible file");
    try:
      self.post_ingestibles(name=files[0], force_reingest='')
      self.verify_http_code_is(CREATED)
    except:
      self.delete_files_in_ingest(files)
      raise
    self.copy_files_to_ingest(files=files)
    try:
      # TODO: response here is not JSON, is that a bug?
      # So I added a param to not parse as json.
      self.post_ingestibles(name=files[0], parse=False)
      self.verify_http_code_is(NOT_ALLOWED)
    except:
      self.verify_ingest_files(exists=files)
      self.delete_files_in_ingest(files)
      raise

  # copy N files from test/data directory to Rhino's ingest directory
  # assuming Rhino is using local file system.
  def copy_files_to_ingest(self, count=1, files=None):
    if not files:
      try:
        files = os.listdir(TEST_DATA_PATH)
        files = [file for file in files if os.path.splitext(file)[1] == ".zip"]
      except:
        raise RuntimeError('error reading directory %r'%(TEST_DATA_PATH,))
      random.shuffle(files)
      files = files[:count]
    for filename in files:
      src, dst = os.path.join(TEST_DATA_PATH, filename), RHINO_INGEST_PATH
      try:
        shutil.copy2(src, dst)
      except:
        raise RuntimeError('error copying from %r to %r'%(src, dst))
    return files

  def delete_files_in_ingest(self, files):
    for filename in files:
      src = os.path.join(RHINO_INGEST_PATH, filename)
      try:
        os.remove(src)
      except:
        raise RuntimeError('error removing from %r'%(src,))

  def verify_ingest_files(self, exists=None, missing=None):
    if exists:
      for filename in exists:
        src = os.path.join(RHINO_INGEST_PATH, filename)
        self.assertTrue(os.path.exists(src), "file is missing in ingest directory: %r"%(src,))
    if missing:
      for filename in missing:
        src = os.path.join(RHINO_INGEST_PATH, filename)
        self.assertFalse(os.path.exists(src), "file exists in ingest directory: %r"%(src,))

if __name__ == '__main__':
  IngestiblesTest._run_tests_randomly()