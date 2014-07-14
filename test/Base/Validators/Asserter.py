#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

'''
This class holds some assert methods for easy use in any class that does not inherits from TestCase
'''


class Asserter(object):

  def assertEquals(self, obj1, obj2):
    assert obj1 == obj2, '"%s" does not match "%s"!' % (obj1, obj2)

  def assertIsNotNone(self, obj):
    assert obj is not None, '"%s" was expected to be None, but was not!' % obj
