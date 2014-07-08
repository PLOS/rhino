#!/usr/bin/env python2

'''
Module to store various decorators that will come in handy while service testing
'''

__author__ = 'jkrzemien@plos.org'

from functools import wraps
from unittest import TestCase
import time


def ensure_api_called(func):

  '''
  Function decorator.
  Used to verify the existance of a response (via get_response() method invocation)
  in the instance that holds the wrapped method.
  Will fail test case being run if no response is present, since can't validate anything over a
  None instance.
  If response is present, will forward call to decorated function.
  '''

  @wraps(func)
  def _exec(value, *args, **kw):
    if value.get_response() is None:
      TestCase.fail(value, 'You MUST invoke an API first, BEFORE performing any validations!')
    else:
      #print 'Executing "%s" validation...' % func.__name__
      return func(value, *args, **kw)

  return _exec


def timeit(method):

  '''
  Function decorator.
  Allows to measure the execution times of dedicated methods
  (module-level methods or class methods) by just adding the
  @timeit decorator in in front of the method call.
  '''

  def timed(*args, **kw):
    ts = time.time()
    result = method(*args, **kw)
    te = time.time()

    print 'Method %r %r call took %2.2f sec...' % (method.__name__, args[1:], te-ts)
    return result

  return timed