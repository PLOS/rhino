#!/usr/bin/env python2

"""
"""

__author__ = 'jkrzemien@plos.org'

from abc import ABCMeta, abstractmethod

from ..Validators.Assert import Assert


class AbstractResponse(object):

  __metaclass__ = ABCMeta

  @abstractmethod
  def get_doi_from_response(self):
    pass

  @abstractmethod
  def get_article_xml_section(self):
    pass

  @abstractmethod
  def get_article_pdf_section(self):
    pass

  @abstractmethod
  def get_graphics_section(self):
    pass

  @abstractmethod
  def get_figures_section(self):
    pass

  @abstractmethod
  def get_syndications_section(self):
    pass

  @abstractmethod
  def get_state(self):
    pass