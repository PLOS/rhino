insert into journal(currentIssueUri, journalKey, eIssn, imageUri,
  title, description, created, lastModified)
select
  currentIssueUri,
  journalKey,
  eIssn,
  imageUri,
  title,
  description,
  now(),
  now()
from
  Journal;

insert into volume(volumeUri, journalID, journalSortOrder, displayName, imageUri, title,
  description, created, lastModified)
select
  v.aggregationUri,
  j.journalID,
  jvl.sortOrder,
  v.displayName,
  v.imageUri,
  v.title,
  v.description,
  v.created,
  now()
from
  Volume v
  join JournalVolumeList jvl on jvl.volumeUri = v.aggregationUri
  join AggregationDetachedCriteria agdc on agdc.aggregationUri = jvl.aggregationUri
  join CriteriaList cl on cl.criteriaUri = agdc.detachedCriteriaUri
  join Criteria c on c.criteriaUri = cl.eqCriterionUri and c.fieldName = 'eIssn'
  left join journal j on j.eIssn = c.value;

insert into issue(issueUri, volumeID, volumeSortOrder, displayName, respectOrder, imageUri, title,
  description, created, lastModified)
select
  i.aggregationUri,
  v.volumeID,
  vis.sortOrder,
  i.displayName,
  i.respectOrder,
  i.imageUri,
  i.title,
  i.description,
  ifnull(i.created, now()),
  now()
from
  Issue i
  join VolumeIssueList vis on i.aggregationUri = vis.issueUri
  join volume v on vis.aggregationUri = v.volumeUri;

insert into issueArticleList(issueID, sortOrder, doi)
select
  i.issueID,
  sortOrder,
  ial.articleUri
from
  IssueArticleList ial
  join issue i on i.issueUri = ial.aggregationUri
  left join article a on a.doi = ial.articleUri;

insert into issueArticleList(issueID, sortOrder, doi)
select distinct
  i.issueID,
  agsc.sortOrder,
  agsc.uri
from
  issue i
  join AggregationSimpleCollection agsc on i.issueUri = agsc.aggregationArticleUri
where
  i.issueID not in (select issueID from issueArticleList);

update
  journal j,
  issue i
set
  j.currentIssueID = i.issueID
where
  j.currentIssueUri = i.issueUri;

alter table journal drop column currentIssueUri;

insert into articlePublishedJournals (articleID, journalID)
select
  a.articleID,
  j.journalID
from journal j
  join Criteria c on j.eIssn = c.value and c.fieldName = 'eIssn'
  join CriteriaList cl on cl.eqCriterionUri = c.criteriaUri
  join AggregationDetachedCriteria agdc on cl.criteriaUri = agdc.detachedCriteriaUri
  join AggregationSimpleCollection agsc on agsc.aggregationArticleUri = agdc.aggregationUri
  join article a on a.doi = agsc.uri;

insert into articlePublishedJournals (articleID, journalID)
select
  a.articleID,
  j.journalID
from
  article a
  join journal j on a.eIssn = j.eIssn;
