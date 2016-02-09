create table pingback (
    pingbackID bigint not null auto_increment,
    articleID bigint not null,
    url varchar(255) CHARACTER SET utf8 COLLATE utf8_bin not null,
    title varchar(255) CHARACTER SET utf8 COLLATE utf8_bin null,
    lastModified datetime not null,
    created datetime not null,
    primary key (pingbackID),
    unique (articleID, url)
);
