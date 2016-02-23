insert into articleAsset
  (articleID, doi, contextElement, contentType, extension, title, description, size,
  sortOrder, created, lastModified)
select distinct
 a.articleID, o.objectInfoUri, o.contextElement, r.contentType, r.name, d.title, d.description,
 r.size, apj.sortOrder,
 CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
from
 article a
 join ObjectInfo o on a.doi = o.isPartOf
 join DublinCore d on o.dublinCoreIdentifier = d.articleUri
 join ArticlePartsJoinTable apj on apj.objectInfoUri = o.objectInfoUri
 join ObjectInfoRepresentationsJoinTable oij on o.objectInfoUri = oij.articleUri
 join Representation r on oij.representationsUri = r.representationUri
 order by apj.sortOrder, r.name;

-- These are weird supp-info assets that have the same doi as the article
insert into articleAsset
  (articleID, doi, contextElement, contentType, extension, size, sortOrder, created, lastModified)
select distinct
 a.articleID, a.doi, null, r.contentType, r.name, r.size, 1, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
from
 article a
 join Representation r on a.doi = r.objectInfoUri
 where r.name not in ('XML', 'PDF');

insert into category (mainCategory, subCategory, created, lastModified)
 select distinct mainCategory, subCategory, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() from Category;

insert into articleCategoryJoinTable (articleID, categoryID)
 select distinct a.articleID, c.categoryID
 from article a
 join ArticleCategoryJoinTable acj on a.doi = acj.articleUri
 join Category old_c on old_c.categoryUri = acj.articleCategoryUri
 join category c on c.mainCategory = old_c.mainCategory and c.subCategory <=> old_c.subCategory;

rename table ArticleCategoryJoinTable to ArticleCategoryJoinTable_old;

alter table articleCategoryJoinTable
  add constraint foreign key (articleID) references article(articleID),
  add constraint foreign key (categoryID) references category(categoryID);

insert into articleCollaborativeAuthors(articleID, sortOrder, name)
select a.articleID, ca.sortOrder, ca.authorName
from article a
 join DublinCore d on a.doi = d.articleUri
 join CollaborativeAuthors ca on d.bibliographicCitationUri = ca.citationUri;

insert into articlePerson(articleID, type, fullName, givenNames, surnames, suffix, sortOrder, created, lastModified)
select a.articleID, 'editor', ac.fullName, ac.givenNames, ac.surnames, ac.suffix,
  aej.sortOrder, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
 from article a
 join ArticleEditorJoinTable aej on aej.articleUri = a.doi
 join ArticleContributor ac on aej.contributorUri = ac.contributorUri;


insert into articlePerson(articleID, type, fullName, givenNames, surnames, suffix, sortOrder, created, lastModified)
select a.articleID, 'author', ac.fullName, ac.givenNames, ac.surnames, ac.suffix,
  aaj.sortOrder, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
 from article a
 join ArticleAuthorJoinTable aaj on aaj.articleUri = a.doi
 join ArticleContributor ac on aaj.contributorUri = ac.contributorUri;

insert into articleRelationship (parentArticleID, otherArticleDoi, otherArticleID, type,
  sortOrder, created, lastModified)
select a.articleID, ar.articleUri, a1.articleID, ar.relationType, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
from article a
 join ArticleRelatedJoinTable arj on arj.articleUri = a.doi
 join ArticleRelated ar on ar.articleRelatedUri = arj.articleRelatedUri
 left outer join article a1 on a1.doi = ar.articleUri;

insert into articleType(articleID, type)
  select a.articleID, at.typeUri from article a join ArticleTypes at on at.articleUri = a.doi;

insert into citedArticle(articleID, uri, keyColumn, year, displayYear, month,
  day, volumeNumber, volume, issue, title, publisherLocation, publisherName,
  pages, eLocationID, journal, note, url, doi, citationType, summary,
  sortOrder, created, lastModified)
select
  a.articleID, c.citationUri, c.keyColumn, c.year, c.displayYear, c.month,
  c.day, c.volumeNumber, c.volume, c.issue, c.title, c.publisherLocation, c.publisherName,
  c.pages, c.eLocationId, c.journal, c.note, c.url, c.doi, c.citationType, c.summary,
  dcr.sortOrder, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
from
  article a
  join DublinCoreReferences dcr on dcr.articleUri = a.doi
  join Citation c on dcr.citationUri = c.citationUri;

insert into citedArticleCollaborativeAuthors(citedArticleID, sortOrder, name)
select ca.citedArticleID, c.sortOrder, c.authorName
from citedArticle ca join CollaborativeAuthors c on c.citationUri = ca.uri;

insert into citedPerson(citedArticleID, type, fullName, givenNames,
  surnames, suffix, sortOrder, created, lastModified)
  select ca.citedArticleID, 'author', p.fullName, p.givenNames,
    p.surnames, p.suffix, a.sortOrder, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
  from citedArticle ca
  join CitationAuthors a on ca.uri = a.citationUri
  join CitedPerson p on a.authorUri = p.citedPersonUri;

insert into citedPerson(citedArticleID, type, fullName, givenNames,
  surnames, suffix, sortOrder, created, lastModified)
  select ca.citedArticleID, 'editor', p.fullName, p.givenNames,
    p.surnames, p.suffix, e.sortOrder, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
  from citedArticle ca
  join CitationEditors e on ca.uri = e.citationUri
  join CitedPerson p on e.editorUri = p.citedPersonUri;

alter table citedArticle drop column uri;

insert into syndication (doi, target, status, submissionCount, errorMessage,
  lastSubmitTimestamp, created, lastModified)
  select s.articleUri, s.target, s.status, s.submissionCount, s.errorMessage,
    case when s.submitTimeStamp is not null then s.submitTimeStamp else s.statusTimeStamp end,
    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
   from Syndication s;

alter table Issue
  add column title text,
  add column description text,
  add column created datetime null,
  drop foreign key Issue_ibfk_1,
  drop foreign key Issue_ibfk_2,
  drop column editorialBoardUri,
  drop column dublinCoreIdentifier;

alter table Journal
  add column title text,
  add column description text,
  add column created datetime null,
  drop foreign key Journal_ibfk_1,
  drop foreign key Journal_ibfk_2,
  drop column editorialBoardUri,
  drop column dublinCoreIdentifier;

alter table Volume
  add column title text,
  add column description text,
  add column created datetime null,
  drop foreign key Volume_ibfk_1,
  drop foreign key Volume_ibfk_2,
  drop column editorialBoardUri,
  drop column dublinCoreIdentifier;

update Issue, DublinCore
 set Issue.title = DublinCore.title, Issue.description = DublinCore.description, Issue.created = DublinCore.created
 where Issue.imageUri = DublinCore.articleUri;

update Journal, DublinCore
 set Journal.title = DublinCore.title, Journal.description = DublinCore.description, Journal.created = DublinCore.created
 where Journal.aggregationUri = DublinCore.articleUri;

update Volume, DublinCore
 set Volume.title = DublinCore.title, Volume.description = DublinCore.description, Volume.created = DublinCore.created
 where Volume.aggregationUri = DublinCore.articleUri;