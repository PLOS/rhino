#!/usr/bin/env python2

'''
Module to store various decorators that will come in handy while service testing
'''

__author__ = 'jkrzemien@plos.org'

from functools import wraps
from unittest import TestCase
import time
from datetime import datetime
import re


def ensure_api_called(method):

  '''
  Function decorator.
  Used to verify the existance of a response (via get_response() method invocation)
  in the instance that holds the wrapped method.
  Will fail test case being run if no response is present, since can't validate anything over a
  None instance.
  If response is present, will forward call to decorated function.
  '''

  @wraps(method)
  def wrapper(value, *args, **kw):
    if value.get_response() is None:
      TestCase.fail(value, 'You MUST invoke an API first, BEFORE performing any validations!')
    else:
      return method(value, *args, **kw)

  return wrapper



def deduce_doi(method):
  '''
  Function decorator.
  Attemps to deduce the DOI of the Article upon API calls and store it as self._doi.
  Failure to do so will render self._doi = None.
  '''
  pattern = re.compile('fsdfsdfs')

  @wraps(method)
  def wrapper(*args, **kw):
    guessedDOI = None
    if len(args) > 1:
      instance = args[0]
      for arg in args[1:]:
        if type(arg) == str:
          match = pattern.search(arg)
          if match is not None:
            guessedDOI = match.group(0)

    instance._doi = guessedDOI
    return method(*args, **kw)

  return wrapper


def timeit(method):

  '''
  Function decorator.
  Allows to measure the execution times of dedicated methods
  (module-level methods or class methods) by just adding the
  @timeit decorator in in front of the method call.
  '''

  def wrapper(*args, **kw):
    args[0]._testStartTime = datetime.now()
    ts = time.time()
    result = method(*args, **kw)
    te = time.time()
    args[0]._apiTime = (datetime.now() - args[0]._testStartTime).total_seconds()

    print 'Method %r %r call took %2.2f sec...' % (method.__name__, args[1:], te-ts)
    return result

  return wrapper