#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

'''
This class holds some assert methods for easy use in any class that does not inherits from TestCase
'''


class Assert(object):

  @staticmethod
  def equals(obj1, obj2):
    assert obj1 == obj2, '"%s" does not match "%s"!' % (obj1, obj2)

  @staticmethod
  def isTrue(expression):
    assert expression == True, 'Expression expected to be True but was "%s"!' % expression

  @staticmethod
  def isNotNone(obj):
    assert obj is not None, '"%s" was expected to be None, but was not!' % obj
