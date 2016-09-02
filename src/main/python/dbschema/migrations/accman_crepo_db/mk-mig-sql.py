#!/usr/bin/env python
import os, sys

create = """
DROP Table IF EXISTS `objkey_uuid`;
CREATE TABLE `objkey_uuid` (
  `objkey` varchar(255) COLLATE utf8_bin NOT NULL,
  `uuid` char(36) COLLATE utf8_bin NOT NULL,
  `size` int(11),
  PRIMARY KEY (`objkey`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
SET autocommit = 0;
START TRANSACTION;
"""
insert = 'INSERT INTO objkey_uuid (objkey, uuid, size) VALUES("{k}","{u}", {s});'
print(create)

# Skip the first line
sys.stdin.readline()

cflag = 1
for r in sys.stdin:
   objkey, uuid, sz,  _ = r.split(',')
   print(insert.format(k=objkey, u=uuid, s=sz))
   cflag += 1
   if (cflag % 100) == 0:
      print('COMMIT;')
#      print('START TRANSACTION;')

print('COMMIT;')

