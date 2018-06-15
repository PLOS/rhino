#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# Copyright (c) 2017 Public Library of Science
#
# Permission is hereby granted, free of charge, to any person obtaining a
# copy of this software and associated documentation files (the "Software"),
# to deal in the Software without restriction, including without limitation
# the rights to use, copy, modify, merge, publish, distribute, sublicense,
# and/or sell copies of the Software, and to permit persons to whom the
# Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
# THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
# DEALINGS IN THE SOFTWARE.

"""
Module to store various Decorators that will come in handy while service testing
"""

from functools import wraps
from unittest import TestCase
from datetime import datetime
import logging
import time

__author__ = 'jkrzemien@plos.org'


class needs(object):
    """
    Decorator to guarantee a given attribute is **present** in an instance.
    If the attribute is not present, test fails with a message containing instructions of which
    method was not called from test to create the required attribute.
    If the attribute is present this decorator does nothing.
    """

    def __init__(self, attribute, needs_method):
        self.attributeNeeded = attribute
        self.methodToInvoke = needs_method

    def __call__(self, method):
        @wraps(method)
        def wrapper(value, *args, **kw):
            if not hasattr(value, self.attributeNeeded):
                TestCase.fail(value, 'You MUST invoke {0} first, BEFORE performing any '
                                     'validations!'.format(self.methodToInvoke))
            else:
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
        setattr(value, 'testStartTime', datetime.now())
        ts = time.time()
        result = method(value, *args, **kw)
        te = time.time()
        setattr(value, 'apiTime', (datetime.now() - value.testStartTime).total_seconds())
        logging.info('')
        logging.info('Method %r %r call took %2.2f sec...' % (method.__name__, args[:], te - ts))
        return result

    return wrapper
