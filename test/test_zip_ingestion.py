#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

'''
This test case validates Rhino's convenience zipUpload API for ZIP ingestion.

Notes:

* For Data-Driven Testing (DDT) you can use ddt, available via: pip install ddt

Decorate your test class with @ddt and @data for your test methods, you will also need to pass an
extra argument to the test method.

* Using Nose's parameterized feature is not recommended since Nose doesn't play nice
with subclasses.

* Still need to take a look @ https://code.google.com/p/mogilefs/wiki/Clients
for MogileFS's Python client implementations.
'''

from Base.IngestibleZipBaseTest import IngestibleZipBaseTest
from Base.Database import Database


class ZipIngestionTest(IngestibleZipBaseTest):

  '''
  Attempting to test as much values as possible without hard coding them
  Ideally, test should:
    * Test data inserted in DB
    * Test files properly stored on MongilFS
  '''

  def test_zip_ingestion_happy_path(self):
    '''
    Validate Rhino's ZIP Upload (Ingestion) API, forced.
    '''

    self.zipUpload('data/pone.0097823.zip', 'forced')
    self.verify_HTTP_code_is(201)
    self.verify_state_is('ingested')
    self.verify_doi_is_correct()
    self.verify_article_xml_section()
    self.verify_article_pdf_section()
    self.verify_graphics_section()
    self.verify_figures_section()

    # Right now I can't access MySQL database at iad-leo-devstack01.int.plos.org from my box.
    print 'Here we have a nice SQL query returned meanwhile: %s ' % \
    Database().query('SELECT Version()')

if __name__ == '__main__':
    IngestibleZipBaseTest._run_tests_randomly()










'''
Actual test against db should validate:

<hibernate-mapping package="org.ambraproject.models" default-lazy="false">

  <class name="Article" table="article">
    <id name="ID" column="articleID" type="long">
      <generator class="native"/>
    </id>

    <timestamp name="lastModified" column="lastModified"/>
    <property name="created" column="created" type="timestamp" not-null="true" update="false"/>

    <property name="doi" column="doi" type="string" not-null="true" unique="true"/>
    <property name="title" column="title" type="text"/>
    <property name="eIssn" column="eIssn" type="string"/>
    <property name="state" column="state" type="integer"/>
    <property name="archiveName" column="archiveName" type="string"/>
    <property name="description" column="description" type="text"/>
    <property name="rights" column="rights" type="text"/>
    <property name="language" column="language" type="string"/>
    <property name="format" column="format" type="string"/>
    <property name="pages" column="pages" type="string"/>
    <property name="eLocationId" column="eLocationId" type="string"/>
    <property name="url" column="url" type="string"/>
    <property name="strkImgURI" column="strkImgURI" type="string"/>

    <property name="date" column="date" type="timestamp"/>

    <property name="volume" column="volume" type="string"/>
    <property name="issue" column="issue" type="string"/>
    <property name="journal" column="journal" type="string"/>

    <property name="publisherLocation" column="publisherLocation" type="string"/>
    <property name="publisherName" column="publisherName" type="string"/>

    <set name="types" table="articleType" cascade="all-delete-orphan">
      <key column="articleID"/>
      <element column="type" type="string"/>
    </set>

    <list name="relatedArticles" cascade="all-delete-orphan">
      <key column="parentArticleID" not-null="true"/>
      <list-index column="sortOrder"/>
      <one-to-many class="ArticleRelationship"/>
    </list>

    <list name="assets" cascade="all-delete-orphan">
      <key column="articleID"/>
      <list-index column="sortOrder"/>
      <one-to-many class="ArticleAsset"/>
    </list>

    <!--Don't want to delete orphan on these-->
    <set name="categories" cascade="save-update" table="articleCategoryJoinTable">
      <key column="articleID"/>
      <many-to-many class="org.ambraproject.models.Category" column="categoryID"/>
    </set>

    <list name="citedArticles" cascade="all-delete-orphan" lazy="true">
      <key column="articleID"/>
      <list-index column="sortOrder"/>
      <one-to-many class="CitedArticle"/>
    </list>

    <list name="collaborativeAuthors" table="articleCollaborativeAuthors" cascade="all-delete-orphan">
      <key column="articleID"/>
      <list-index column="sortOrder"/>
      <element column="name" type="string"/>
    </list>

    <list name="authors"
          cascade="all-delete-orphan"
          where="type = 'author'">
      <key column="articleID"/>
      <list-index column="sortOrder"/>
      <one-to-many class="ArticleAuthor"/>
    </list>

    <list name="editors"
          cascade="all-delete-orphan"
          where="type = 'editor'">
      <key column="articleID"/>
      <list-index column="sortOrder"/>
      <one-to-many class="ArticleEditor"/>
    </list>

    <set name="journals" table="articlePublishedJournals" cascade="none" lazy="true">
      <key column="articleID"/>
      <many-to-many class="Journal" column="journalID"/>
    </set>

  </class>

</hibernate-mapping>
'''

