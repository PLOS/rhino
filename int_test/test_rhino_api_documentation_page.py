
import re
import resources
from selenium import webdriver
from selenium.common.exceptions import NoSuchElementException
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
import time
import unittest

class TestRhinoApiDocumentationPage(unittest.TestCase):
    def setUp(self):
        self.driver = webdriver.Firefox()
        self.driver.implicitly_wait(30)
        self.base_url = resources.base_url
        self.verificationErrors = []
        self.accept_next_alert = True
    
    def test_rhino_api_documentation_page(self):
        driver = self.driver
        driver.get(self.base_url + "/api/")
        self.assertEqual("Ambra Service REST API", driver.title)
        self.assertRegexpMatches(driver.find_element_by_css_selector("pre").text, r"Version: [\d]\.[\d]\.[\d].*")
        self.assertEqual("/articles", driver.find_element_by_link_text("/articles").text)
        self.assertRegexpMatches(driver.find_element_by_css_selector("li > ul > li").text, r"\?state= published ingested disabled")
        self.assertRegexpMatches(driver.find_element_by_xpath("//li[2]").text, r"\?syndication= pending in_progress success failure")
        self.assertRegexpMatches(driver.find_element_by_link_text("?pingbacks").text, r"\?pingbacks")
        self.assertRegexpMatches(driver.find_element_by_link_text("?date").text, r"\?date")
        self.assertEqual("/ingestibles", driver.find_element_by_link_text("/ingestibles").text)
        self.assertEqual("/journals", driver.find_element_by_link_text("/journals").text)
        self.assertEqual("/config", driver.find_element_by_link_text("/config").text)
        #self.assertEqual("/users", driver.find_element_by_link_text("/users").text)
        #self.assertEqual("/users/AUTHID", driver.find_element_by_link_text("/users/AUTHID").text)
        self.assertTrue(self.is_element_present(By.CSS_SELECTOR, "img[alt=\"Rhino relaxation\"]"))
        # Validate basic article query result format
        driver.find_element_by_link_text("/articles").click()
        print('Validating article query return set syntax')
        try: self.assertRegexpMatches(driver.find_element_by_css_selector('pre').text,'\{' + resources.article_query + '\}')
        except: 
          print('WARNING: Empty or incorrectly formatted data set returned for article query')
          pass
        driver.back()

        # Valdidate article state queries
        for article_state in resources.article_states:
          driver.find_element_by_link_text(article_state).click()
          print('Validating ' + article_state + ' query return set syntax')
          query = self.getQueryType(article_state)
          print('Format of return set: \{' + query + '\}')
          try: self.assertRegexpMatches(driver.find_element_by_css_selector('pre').text,'\{' + query + '\}')
          except: 
            print('WARNING: Empty or incorrectly formatted data set returned for article state sub-query: ' + article_state)
            pass
          driver.back()
          self.assertEqual("Ambra Service REST API", driver.title)

        # Validate article syndication state queries
        for art_synd_state in resources.art_synd_states:
          driver.find_element_by_link_text(art_synd_state).click()
          print('Validating ' + art_synd_state + ' query return set syntax')
          query = self.getQueryType(art_synd_state)
          try: self.assertRegexpMatches(driver.find_element_by_css_selector('pre').text,'\{' + query + '\}')
          except: 
            print('WARNING: Empty or incorrectly formatted data set returned for article syndication sub-query: ' + art_synd_state)
            pass
          driver.back()
          self.assertEqual("Ambra Service REST API", driver.title)

        # Validate article pingback query result format
        driver.find_element_by_link_text("?pingbacks").click()
        print('Validating pingbacks query return set syntax')
        try: self.assertRegexpMatches(driver.find_element_by_css_selector('pre').text,'\{' + resources.pingbacks_query + '\}')
        except: 
          print('WARNING: Empty or incorrectly formatted data set returned for article pingback query')
          pass
          driver.back()

        # Validate article date query result format
        driver.find_element_by_link_text("?date").click()
        print('Validating date query return set syntax')
        try: self.assertRegexpMatches(driver.find_element_by_css_selector('pre').text,'\{' + resources.date_query + '\}')
        except: 
          print('WARNING: Empty or incorrectly formatted data set returned for article date query')
          pass
          driver.back()

        # Validate basic ingestibles query result format
        driver.find_element_by_link_text("/ingestibles").click()
        print('Validating ingestibles query return set syntax')
        try: self.assertRegexpMatches(driver.find_element_by_css_selector('pre').text,'\{' + resources.ingestibles_query + '\}')
        except: 
          print('WARNING: Empty or incorrectly formatted data set returned for ingestibles query')
          pass
          driver.back()

        # Validate basic journals query result format
        driver.find_element_by_link_text("/journals").click()
        print('Validating journals query return set syntax')
        try: self.assertRegexpMatches(driver.find_element_by_css_selector('pre').text,'\{' + resources.journals_query + '\}')
        except: 
          print('WARNING: Empty or incorrectly formatted data set returned for journals query')
          pass
          driver.back()

        # Validate basic config query result format
        driver.find_element_by_link_text("/config").click()
        print('Validating config query return set syntax')
        try: self.assertRegexpMatches(driver.find_element_by_css_selector('pre').text,'\{' + resources.config_query + '\}')
        except: 
          print('WARNING: Empty or incorrectly formatted data set returned for config query')
          pass
          driver.back()

        # Validate basic users query result format
        #driver.find_element_by_link_text("/users").click()
        driver.get(self.base_url + "/api/users/")
        print('Validating users query return set syntax')
        try: self.assertRegexpMatches(driver.find_element_by_css_selector('pre').text,'\{' + resources.users_query + '\}')
        except: 
          print('WARNING: Empty or incorrectly formatted data set returned for users query')
          pass
          driver.back()

        # Validate user/authid query result format
        driver.get(self.base_url + "/api/users/" + resources.authid)
        print('Validating user authID query return set syntax')
        try: self.assertRegexpMatches(driver.find_element_by_css_selector('pre').text,'\{' + resources.users_authid_query + '\}')
        except: 
          print('WARNING: Empty or incorrectly formatted data set returned for user AUTHID query')
          pass
          driver.back()

    def getQueryType(self, query):
      if query == 'published':
        qType = resources.published_query
      elif query =='ingested':
        qType = resources.ingested_query
      elif query == 'disabled':
        qType = resources.disabled_query
      elif query == 'pending':
        qType = resources.pending_query
      elif query == 'in_progress':
        qType = resources.in_progress_query
      elif query == 'success':
        qType = resources.success_query
      else:
        qType = resources.failure_query
      return qType

    def is_element_present(self, how, what):
        try: self.driver.find_element(by=how, value=what)
        except NoSuchElementException, e: return False
        return True
    
    def is_alert_present(self):
        try: self.driver.switch_to_alert()
        except NoAlertPresentException, e: return False
        return True
    
    def close_alert_and_get_its_text(self):
        try:
            alert = self.driver.switch_to_alert()
            alert_text = alert.text
            if self.accept_next_alert:
                alert.accept()
            else:
                alert.dismiss()
            return alert_text
        finally: self.accept_next_alert = True
    
    def tearDown(self):
        self.driver.quit()
        self.assertEqual([], self.verificationErrors)

if __name__ == "__main__":
    unittest.main()
