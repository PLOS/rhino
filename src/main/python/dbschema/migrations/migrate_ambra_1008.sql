alter table annotation drop foreign key annotation_ibfk_4;

alter table annotationFlag drop foreign key annotationFlag_ibfk_2;

alter table articleCategoryFlagged drop foreign key articleCategoryFlagged_ibfk_3;

alter table userArticleView drop foreign key userArticleView_ibfk_1;

alter table userSearch drop foreign key userSearch_ibfk_1;

alter table userLogin drop foreign key userLogin_ibfk_1;

alter table userRolePermission drop foreign key userRolePermission_ibfk_1;

drop table userOrcid;
drop table userProfileMetaData;
drop table userProfileRoleJoinTable;
drop table userRole;
drop table savedSearch;
drop table savedSearchQuery;
drop table userProfile;