update Annotation set annotates = 'info:doi/10.1371/journal.pone.0000401'
where annotationUri = 'info:doi/10.1371/annotation/336b066d-b07b-4a10-b4d1-44dda0c40810';

insert into
 annotation(annotationURI, articleID, parentID,
  userProfileID, type, title, body, xpath,
  competingInterestBody, created, lastModified)
select
  ann.annotationUri,
  a.articleID,
  null,
  up.userProfileID,
  ann.webType,
  ann.title,
  ab.body,
  ann.context,
  ab.ciStatement,
  ann.creationDate,
  ann.creationDate
from
 Annotation ann
 join userProfile up on up.userAccountUri = ann.creator
 left outer join article a on a.doi = ann.annotates
 left outer join AnnotationBlob ab on ann.body = ab.blobUri
 left outer join Annotation flag on flag.annotationUri = ann.annotates
 left outer join Reply replyflag on replyflag.replyUri = ann.annotates
where
 ann.webType in ('Note','Comment','MinorCorrection','Retraction','FormalCorrection')
 and flag.annotationUri is null and replyflag.replyUri is null;

insert into
 annotation(annotationURI, articleID, parentID,
  userProfileID, type, title, body, xpath,
  competingInterestBody, created, lastModified)
select
  r.replyUri,
  a.articleID,
  null,
  up.userProfileID,
  r.webType,
  r.title,
  rb.body,
  null,
  rb.ciStatement,
  r.creationDate,
  r.creationDate
from
  Reply r
  join Annotation ann on r.root = ann.annotationUri
  join article a on ann.annotates = a.doi
  join ReplyBlob rb on r.body = rb.blobUri
  join userProfile up on r.creator = up.userAccountUri;

update
  annotation a,
  Reply r,
  annotation ab
set
  a.parentID = ab.annotationID
where
  a.annotationUri = r.replyUri
  and r.inReplyTo = ab.annotationUri;

delete from annotation where type = 'Reply' and parentID is null;

alter table annotationCitation add column temp_citationUri varchar(255);

insert into annotationCitation (temp_citationUri, year, volume, issue, journal,
  title, publisherName, eLocationId, note, url, summary, created, lastModified)
select
  c.citationUri,
  c.year,
  c.volume,
  c.issue,
  c.journal,
  c.title,
  c.publisherName,
  c.eLocationId,
  c.note,
  c.url,
  c.summary,
  a.creationDate,
  a.creationDate
from
  Annotation a
  join Citation c on a.bibliographicCitationUri = c.citationUri;

insert into annotationCitationAuthor
  (annotationCitationID, fullName, givenNames,
  surnames,suffix, sortOrder, created, lastModified)
select
  cit.annotationCitationID,
  ac.fullName,
  ac.givenNames,
  ac.surnames,
  ac.suffix,
  acjt.sortOrder,
  ann.creationDate,
  ann.creationDate
from Annotation ann
  join AnnotationAuthorCitationJoinTable acjt on ann.bibliographicCitationUri = acjt.citationUri
  join ArticleContributor ac on acjt.contributorUri = ac.contributorUri
  join annotationCitation cit on acjt.citationUri = cit.temp_citationUri;

insert into annotationCitationCollabAuthor(
  annotationCitationID,name,sortOrder)
select
  cit.annotationCitationID,
  ca.authorName,
  ca.sortOrder
from Annotation a
  join CollaborativeAuthors ca on a.bibliographicCitationUri = ca.citationUri
  join annotationCitation cit on ca.citationUri = cit.temp_citationUri;

update annotation a, Annotation ann, annotationCitation ac set a.annotationCitationID = ac.annotationCitationID
  where a.annotationURI = ann.annotationUri
  and ann.bibliographicCitationUri = ac.temp_citationUri;

alter table annotationCitation drop column temp_citationUri;

insert into
 annotation(annotationURI, articleID, parentID,
  userProfileID, type, title, body, xpath,
  competingInterestBody, created, lastModified)
select
  ann.annotationUri,
  a.articleID,
  null,
  up.userProfileID,
  'Rating',
  rc.commentTitle,
  rc.commentValue,
  null,
  rc.ciStatement,
  ann.creationDate,
  ann.creationDate
from
  RatingContent rc
  join Annotation ann on rc.ratingContentUri = ann.body
  join article a on a.doi = ann.annotates
  join userProfile up on up.userAccountUri = ann.creator;

insert into rating(annotationID, insight, reliability, style, singleRating)
select
  a.annotationID,
  rc.insightValue,
  rc.reliabilityValue,
  rc.styleValue,
  rc.singleRatingValue
from
  annotation a
  join Annotation ann on a.annotationUri = ann.annotationUri
  join RatingContent rc on rc.ratingContentUri = ann.body;

insert into ratingSummary(articleID, insightNumRatings, insightTotal, reliabilityNumRatings,
  reliabilityTotal, styleNumRatings, styleTotal, singleRatingNumRatings, singleRatingTotal,
  usersThatRated, created, lastModified)
select
  a.articleID,
  rsc.insightNumRatings,
  rsc.insightTotal,
  rsc.reliabilityNumRatings,
  rsc.reliabilityTotal,
  rsc.styleNumRatings,
  rsc.styleTotal,
  rsc.singleRatingNumRatings,
  rsc.singleRatingTotal,
  rsc.numUsersThatRated,
  ann.creationDate,
  ann.creationDate
from
  RatingSummaryContent rsc
  join Annotation ann on ann.body = rsc.ratingSummaryContentUri
  join article a on a.doi = ann.annotates;

insert into trackback(articleID, url, title, blogname,
  excerpt, created, lastModified)
select
  a.articleID,
  tbc.url,
  tbc.title,
  tbc.blog_name,
  tbc.excerpt,
  ann.creationDate,
  ann.creationDate
from
  Trackback t
  join Annotation ann on ann.annotationUri = t.trackbackUri
  join TrackbackContent tbc on tbc.trackbackContentUri = ann.body
  join article a on ann.annotates = a.doi;

create table temp_flags select
  ann.annotationUri,
  ann.annotates,
  ann.creator
from
  Annotation ann
  join Annotation flag on ann.annotates = flag.annotationUri and flag.Class in ('Reply','Comment','MinorCorrection','Retraction', 'FormalCorrection', 'Rating')
union select
  flag.annotationUri,
  flag.annotates,
  flag.creator
from
  Annotation flag
  join Reply r on flag.annotates = r.replyUri;

insert into annotationFlag(annotationID, userProfileID, reason, comment, created, lastModified)
select
  new_ann.annotationID,
  up.userProfileID,
  ExtractValue(ab.body,'flag/@reasonCode'),
  ExtractValue(ab.body,'flag/comment'),
  old_ann.creationDate,
  current_timestamp()
from
  temp_flags temp
  join Annotation old_ann on temp.annotationUri = old_ann.annotationUri
  join AnnotationBlob ab on ab.blobUri = old_ann.body
  join annotation new_ann on temp.annotates = new_ann.annotationUri
  join userProfile up on up.userAccountUri = temp.creator;

drop table temp_flags;

