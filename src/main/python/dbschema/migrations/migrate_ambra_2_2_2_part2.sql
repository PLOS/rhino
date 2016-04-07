insert into
  userProfile(userProfileURI, userAccountURI, accountState,
  authId, realName, givenNames, surName, title, gender, email, homePage,
  weblog, publications, displayName, suffix, positionType, organizationName,
  organizationType, organizationVisibility, postalAddress, city, country,
  biography, interests, researchAreas, created, lastModified
)
select
  ua.userProfileUri, ua.userAccountUri, ua.state, aid.value,
  up.realName, up.givenNames, up.surName, up.title, up.gender, replace(up.email,'mailto:',''), up.homePage,
  up.weblog, up.publications, up.displayName, up.suffix, up.positionType,
  up.organizationName, up.organizationType, up.organizationVisibility,
  up.postalAddress, up.city, up.country, up.biographyText,
  up.interestsText, up.researchAreasText,
  now(), now()
from UserAccount ua
  join UserAccountAuthIdJoinTable uajt on uajt.userAccountUri = ua.userAccountUri
  join AuthenticationId aid on aid.authenticationIdUri = uajt.authenticationIdUri
  join UserProfile up on up.userProfileUri = ua.userProfileUri;

create table tempWork
SELECT
  ua.userProfileUri,
  GROUP_CONCAT(upv.value SEPARATOR ',') as val
FROM
  UserAccountPreferencesJoinTable uapjt
  join UserAccount ua on uapjt.userAccountUri = ua.userAccountUri
  join UserPreferencesJoinTable upjt on upjt.userPreferencesUri = uapjt.preferencesUri
  join UserPreference up on upjt.userPreferenceUri = up.userPreferenceUri
  join UserPreferenceValues upv on upv.userPreferenceUri = up.userPreferenceUri
where
  up.name = 'alertsJournals'
GROUP BY
  ua.userProfileUri;

alter table tempWork add index(userProfileUri);

update
 userProfile up,
 tempWork t
set
  up.alertsJournals = t.val,
  up.lastModified = now()
where
  up.userProfileUri = t.userProfileUri;

drop table tempWork;

insert into userRole(roleName,created,lastModified) values('admin',now(),now());

insert into
  userProfileRoleJoinTable (userRoleID, userProfileID)
select
  newR.userRoleID,
  upNew.userProfileID
from
  UserAccount ua
  join UserProfile up on ua.userProfileUri = up.userProfileUri
  join userProfile upNew on upNew.userProfileUri = up.userProfileUri
  join UserAccountRoleJoinTable uarjt on uarjt.userAccountUri = ua.userAccountUri
  join UserRole ur on uarjt.roleUri = ur.userRoleUri
  join userRole newR on newR.roleName = ur.roleUri
where
  ur.roleUri is not null;
