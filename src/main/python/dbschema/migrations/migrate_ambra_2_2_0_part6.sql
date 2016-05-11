drop table ArticleCategoryJoinTable_old;
drop table Category;
drop table ArticleEditorJoinTable;
drop table ArticleAuthorJoinTable;
drop table ArticleRelatedJoinTable;
drop table ArticleRelated;
drop table ArticleTypes;
drop table ReferencedAuthorCitationJoinTable;
drop table ReferencedEditorCitationJoinTable;
drop table CitedPerson;
drop table DublinCoreReferences;
drop table ArticleRepresentationsJoinTable;
drop table ArticlePartsJoinTable;
drop table ObjectInfoRepresentationsJoinTable;
drop table ObjectInfo;
drop table Representation;
drop table Article;
drop table DublinCoreCreators;
drop table DublinCoreContributors;
drop table DublinCoreSummary;
drop table DublinCoreSubjects;
drop table DublinCoreLicenses;
drop table EditorialBoardEditors;
drop table EditorialBoard;
drop table License;
drop table Syndication;

delete ca from
  article a
  join DublinCore d on a.doi = d.articleUri
  join CollaborativeAuthors ca on d.bibliographicCitationUri = ca.citationUri;

delete d,c from
 article a
 join DublinCore d on a.doi = d.articleUri
 join Citation c on d.bibliographicCitationUri = c.citationUri;

drop table DublinCore;

alter table articleCollaborativeAuthors add constraint foreign key (articleID) references article(articleID);

drop trigger sortOrder_articleRelationship;
drop trigger sortOrder_articleAsset;