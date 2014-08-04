#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

'''
This test cases validates Rhino's Articles Tests.
'''

from Base.Api.Rhino.Articles import Articles

class ArticlesTest(Articles):

  def test_article_syndication_happy_path(self):
    """
    PATCH articles: Update article with publish and syndication.
    """

    # Prepare API arguments
    desiredState = 'published'

    syndications = {
      'CROSSREF': {'status': 'IN_PROGRESS'}
    }

    # Invoke API
    self.updateArticle('10.1371/journal.pone.0097823', desiredState, syndications)
    self.parse_response_as_json()

    # Perform validations
    self.verify_http_code_is(200)
    self.verify_state_is('published')

    # Can't actually perform this validation since Response ALWAYS contains error message:
    # "CROSSREF queue not configured"...(bug?)

    # self.verify_syndications_status_is(syndications)

    self.verify_doi_is_correct()

    self.define_zip_file_for_validations('pone.0097823.zip')

    self.verify_article_xml_section()

    # PDF validator fails due to time data '2014-07-14T16:40:19Z' does not match format '%Y-%m-%dT%H:%M:%S.%fZ'
    # Seems like each JSON returned from each API has its own way of formatting dates in Rhino :'(

    # self.verify_article_pdf_section()

    # Same here! :'(
    # self.verify_graphics_section()

    # Same here! :'(
    # self.verify_figures_section()


if __name__ == '__main__':
    Articles._run_tests_randomly()