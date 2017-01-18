#!/usr/bin/env python2

"""
"""

__author__ = 'jgray@plos.org'

from abc import ABCMeta, abstractmethod


class AbstractResponse(object):

  __metaclass__ = ABCMeta

  @abstractmethod
  def get_journals(self):
    pass

  @abstractmethod
  def get_journalKey(self):
    pass

  @abstractmethod
  def get_journaleIssn(self):
    pass

  @abstractmethod
  def get_journalTitle(self):
    pass

  @abstractmethod
  def get_article_doi(self):
    pass

  @abstractmethod
  def get_article_revision_number(self):
    pass

