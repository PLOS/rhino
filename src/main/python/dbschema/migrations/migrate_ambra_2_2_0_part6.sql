/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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