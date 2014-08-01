#!/usr/bin/env python2

"""
Module to store various Decorators that will come in handy while service testing
"""

__author__ = 'jkrzemien@plos.org'

from functools import wraps
from ..Response.JSONResponse import JSONResponse
from ..Response.XMLResponse import XMLResponse


def JSON(cls):
  """
  """
  setattr(cls, '__entityType__', 'JSON')
  return cls


def XML(cls):
  """
  """
  setattr(cls, '__entityType__', 'XML')
  return cls


def store_as_entity(method):
  """
  Function decorator.

  """

  @wraps(method)
  def wrapper(value, *args, **kw):
    entityType = getattr(value, '__entityType__', 'JSON')
    method(value, *args, **kw)
    response = getattr(value, '_http')
    if entityType == 'JSON':
      setattr(value, '_parsed_response', JSONResponse(response.text))
    else:
      setattr(value, '_parsed_response', XMLResponse(response.text))
    return response

  return wrapper

