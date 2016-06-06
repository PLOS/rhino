/*
 * This script will drop all tables whose data has been extracted to the User API (NED).
 * When we are ready to drop formal support for these tables in a future release,
 * change this script's name as appropriate and add it to migrations.json.
 */

drop table userOrcid;
drop table userProfileMetaData;
drop table savedSearch;
drop table savedSearchQuery;
drop table userSearch;
drop table userLogin;
drop table userArticleView;
drop table userProfile;
