#!/usr/bin/env python2

"""
Module to store various Decorators that will come in handy while service testing
"""

__author__ = 'jkrzemien@plos.org'

from functools import wraps
from unittest import TestCase
from datetime import datetime
import time
import re

from ..Response.JSONResponse import JSONResponse
from ..Response.XMLResponse import XMLResponse


def ensure_api_called(method):
  """
  Function decorator.
  Used to verify the existance of a Response (via get_response() method invocation)
  in the instance that holds the wrapped method.
  Will fail test case being run if no Response is present, since can't validate anything over a
  None instance.
  If Response is present, will forward call to decorated function.
  """

  @wraps(method)
  def wrapper(value, *args, **kw):
    if value.get_response() is None:
      TestCase.fail(value, 'You MUST invoke an API first, BEFORE performing any validations!')
    else:
      return method(value, *args, **kw)

  return wrapper


def ensure_zip_provided(method):
  """
  Function decorator.
  Used to verify the existance of a ZIP file loaded (via define_zip_file_for_validations() method invocation)
  in the instance that holds the wrapped method.
  Will fail test case being run if no ZIP file is present, since can't validate anything over a
  None instance.
  If ZIP file is present, will forward call to decorated function.
  """

  @wraps(method)
  def wrapper(value, *args, **kw):
    if not hasattr(value, '_zip') or value._zip is None:
      TestCase.fail(value, 'You MUST define_zip_file_for_validations() first, BEFORE performing any validations!')
    else:
      return method(value, *args, **kw)

  return wrapper


def deduce_doi(method):
  """
  Function decorator.
  Attemps to deduce the DOI of the Article upon API calls and store it as self._doi.
  Failing to do so will render self._doi = None.
  """
  pattern = re.compile('(info:doi/10.1371/)?journal\.p[a-z]+?\.\d+')

  @wraps(method)
  def wrapper(value, *args, **kw):
    guessedDOI = None
    for arg in args:
      if type(arg) == str:
        match = pattern.search(arg)
        if match is not None:
          guessedDOI = match.group(0)

    if guessedDOI and not guessedDOI.startswith('info:doi'):
      value._doi = 'info:doi/10.1371/' + guessedDOI
    else:
      value._doi = guessedDOI

    return method(value, *args, **kw)

  return wrapper


def timeit(method):
  """
  Function decorator.
  Allows to measure the execution times of dedicated methods
  (module-level methods or class methods) by just adding the
  @timeit decorator in in front of the method call.
  """

  @wraps(method)
  def wrapper(value, *args, **kw):
    setattr(value, '_testStartTime', datetime.now())
    ts = time.time()
    result = method(value, *args, **kw)
    te = time.time()
    setattr(value, '_apiTime', (datetime.now() - value._testStartTime).total_seconds())

    print ''
    print 'Method %r %r call took %2.2f sec...' % (method.__name__, args[:], te - ts)
    return result

  return wrapper