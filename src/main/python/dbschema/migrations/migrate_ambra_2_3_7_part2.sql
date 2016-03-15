update userRole set roleName = 'Admin';

SET @admin_id = (select userRoleID from userRole limit 1);

INSERT INTO userRolePermission (userRoleID, permission) VALUES (@admin_id, 'INGEST_ARTICLE');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@admin_id, 'ACCESS_ADMIN');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@admin_id, 'MANAGE_FLAGS');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@admin_id, 'MANAGE_ANNOTATIONS');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@admin_id, 'MANAGE_USERS');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@admin_id, 'MANAGE_ROLES');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@admin_id, 'MANAGE_JOURNALS');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@admin_id, 'MANAGE_SEARCH');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@admin_id, 'MANAGE_CACHES');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@admin_id, 'CROSS_PUB_ARTICLES');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@admin_id, 'DELETE_ARTICLES');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@admin_id, 'VIEW_UNPUBBED_ARTICLES');

UPDATE userRole SET lastModified = now();

INSERT INTO userRole (roleName, created, lastModified) VALUES ('Production', now(), now());
INSERT INTO userRole (roleName, created, lastModified) VALUES ('Editorial', now(), now());

SET @prod_id = (select userRoleID from userRole where roleName = 'Production');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@prod_id, 'INGEST_ARTICLE');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@prod_id, 'ACCESS_ADMIN');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@prod_id, 'MANAGE_FLAGS');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@prod_id, 'MANAGE_ANNOTATIONS');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@prod_id, 'MANAGE_USERS');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@prod_id, 'MANAGE_ROLES');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@prod_id, 'MANAGE_JOURNALS');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@prod_id, 'MANAGE_SEARCH');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@prod_id, 'MANAGE_CACHES');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@prod_id, 'CROSS_PUB_ARTICLES');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@prod_id, 'DELETE_ARTICLES');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@prod_id, 'VIEW_UNPUBBED_ARTICLES');

SET @ed_id = (select userRoleID from userRole where roleName = 'Editorial');
INSERT INTO userRolePermission (userRoleID, permission) VALUES (@ed_id, 'VIEW_UNPUBBED_ARTICLES');

