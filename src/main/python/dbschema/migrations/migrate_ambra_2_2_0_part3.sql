delete from ReferencedAuthorCitationJoinTable
  where citedPersonUri = 'info:doi/10.1371/profile/a2648f40-4341-41ff-8920-b39ab6313f89';

delete from CitedPerson
  where citedPersonUri = 'info:doi/10.1371/profile/a2648f40-4341-41ff-8920-b39ab6313f89';

delete from ArticleCategoryJoinTable
  where articleCategoryUri = 'info:doi/10.1371/category/f6c5fb9b-d121-41f9-a9ce-70b6d2dbc02c';

delete from Category
  where categoryUri = 'info:doi/10.1371/category/f6c5fb9b-d121-41f9-a9ce-70b6d2dbc02c';

-- Duplicated Representation
delete from ObjectInfoRepresentationsJoinTable
  where representationsUri = 'info:doi/10.1371/representation/d7919e50-6b6f-46ff-9abf-cde75d0bfcd5';

delete from Representation
  where representationUri = 'info:doi/10.1371/representation/d7919e50-6b6f-46ff-9abf-cde75d0bfcd5';

/*these categoryUri has new line and spaces in subCategory(hence duplicate data in new category table) so update those before insertion data scripts run. */
update Category set subCategory = 'Health Services Research and Economics' where categoryUri = 'info:doi/10.1371/category/0703c208-7055-407c-ab77-e61d2e8563a1';
update Category set subCategory = 'Quality and Safety in Medical Practice' where categoryUri = 'info:doi/10.1371/category/0da9024d-11e6-4705-9118-5f3de53c0d36';
update Category set subCategory = 'Health Services Research and Economics' where categoryUri = 'info:doi/10.1371/category/69b48ea6-308e-4902-8c87-c6c1dcf9254d';
update Category set subCategory = 'Health Services Research and Economics' where categoryUri = 'info:doi/10.1371/category/b95e7029-8b49-4b90-97ba-54fd1f077c8b';

update Category set subCategory = 'Epidemiology and Control of Infectious Diseases' where categoryUri = 'info:doi/10.1371/category/4cda5e3f-1509-4e58-b771-27e22d525ef4';
update Category set subCategory = 'Epidemiology and Control of Infectious Diseases' where categoryUri = 'info:doi/10.1371/category/89a5975b-ca56-448c-95a8-faaca00eebf9';
update Category set subCategory = 'Epidemiology and Control of Infectious Diseases' where categoryUri = 'info:doi/10.1371/category/a93e67dc-212c-48cb-ad15-93cd0e8646ae';
update Category set subCategory = 'Epidemiology and Control of Infectious Diseases' where categoryUri = 'info:doi/10.1371/category/af31a022-2f24-4336-b1cc-a5e010d81f94';
update Category set subCategory = 'Epidemiology and Control of Infectious Diseases' where categoryUri = 'info:doi/10.1371/category/d13ae09a-9b24-413e-818f-62ee8d6d7ab6';
update Category set subCategory = 'Epidemiology and Control of Infectious Diseases' where categoryUri = 'info:doi/10.1371/category/f9c27aef-57f8-47d3-8539-1842886bd330';

update Category set subCategory = 'Post-Translational Regulation of Gene Expression' where categoryUri = 'info:doi/10.1371/category/2ab8bcb4-ada2-4f01-a2e8-2a27e86dc06d';

update Category set subCategory = 'Social and Behavioral Determinants of Health' where categoryUri = 'info:doi/10.1371/category/c596e61a-dda9-4991-9eb7-527826026b12';
update Category set subCategory = 'Nosocomial and Healthcare-Associated Infections' where categoryUri = 'info:doi/10.1371/category/d17d4404-8257-4335-9d44-6463c73d57ec';
update Category set subCategory = 'Health Services Research and Economics' where categoryUri = 'info:doi/10.1371/category/d52719a6-a95a-4758-96d9-cc2d17c63442';
update Category set subCategory = 'Health Services Research and Economics' where categoryUri = 'info:doi/10.1371/category/d53d64f1-44b1-4b09-86b3-9bdef4542e9f';
update Category set subCategory = 'Social and Behavioral Determinants of Health' where categoryUri = 'info:doi/10.1371/category/efa2a86f-31cc-4be9-8094-6befe3f90468';

update Category set subCategory = 'Autoimmunity, Autoimmune, and Inflammatory Diseases' where categoryUri = 'info:doi/10.1371/category/68e44211-ff58-402a-864b-df5d1b74c60c';

update Category set subCategory = 'Mechanisms of Resistance and Susceptibility, including Host Genetics' where categoryUri = 'info:doi/10.1371/category/7dfa8036-8d7e-4368-a62e-8c7dae5ee912';

update Category set subCategory = 'Infectious Diseases' where subCategory = 'infectious Diseases';

insert into article
 (doi, title, eIssn,
  state, archiveName, description,
  rights, language, format, date,
  volume, issue, journal, pages, eLocationID, publisherLocation,
  publisherName, url, created, lastModified)
select
 a.articleUri, d.title, a.eIssn,
 a.state, a.archiveName, d.description,
 d.rights, d.language, d.format, d.date,
 c.volume, c.issue, c.journal,
 c.pages, c.eLocationId, c.publisherLocation, c.publisherName,
 c.url,
 CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
from
 Article a
 join DublinCore d on a.articleUri = d.articleUri
 join Citation c on d.bibliographicCitationUri = c.citationUri;


-- Migrate the XML and PDF assets, then add a trigger to get the order for the rest of them
insert into articleAsset
  (articleID, doi, contextElement, contentType, extension, size, sortOrder, created, lastModified)
select distinct
 a.articleID, a.doi, null, r.contentType, r.name, r.size, case when r.name = 'XML' then 0 else 1 end, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
from
 article a
 join Representation r on a.doi = r.objectInfoUri
 where r.name in ('XML', 'PDF');