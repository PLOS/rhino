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

