alter table annotation drop foreign key annotation_ibfk_4;

alter table annotationFlag drop foreign key annotationFlag_ibfk_2;

alter table articleCategoryFlagged drop foreign key articleCategoryFlagged_ibfk_3;

alter table userProfileRoleJoinTable drop foreign key userProfileRoleJoinTable_ibfk_2;

drop table userOrcid;
drop table userProfileMetaData;
drop table savedSearch;
drop table savedSearchQuery;
drop table userSearch;
drop table userLogin;
drop table userArticleView;
drop table userProfile;
