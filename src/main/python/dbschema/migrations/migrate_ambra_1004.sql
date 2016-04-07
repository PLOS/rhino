alter table userProfile
  add column passwordReset bit(1) NOT NULL DEFAULT b'0' after password;
