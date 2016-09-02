#!/usr/bin/bash
zcat objkey-uuid.csv.gz | python mk-mig-sql.py > objkey-uuid.sql
split -l 2000000 objkey-uuid.sql objkey-uuid-
mv objkey-uuid-aa objkey-uuid-aa2
echo "COMMIT;" >> objkey-uuid-aa2
cat preamble.sql objkey-uuid-ab  > objkey-uuid-ab2
echo "COMMIT;" >> objkey-uuid-ab2
cat preamble.sql objkey-uuid-ac  > objkey-uuid-ac2
echo "COMMIT;" >> objkey-uuid-ac2
cat preamble.sql objkey-uuid-ad  > objkey-uuid-ad2
echo "COMMIT;" >> objkey-uuid-ad2
cat preamble.sql objkey-uuid-ae  > objkey-uuid-ae2
echo "COMMIT;" >> objkey-uuid-ae2
cat preamble.sql objkey-uuid-af  > objkey-uuid-af2
echo "COMMIT;" >> objkey-uuid-af2

nohup mysql test < objkey-uuid-aa2 > objkey-uuid-aa2.rslt&
sleep 5
nohup mysql test < objkey-uuid-ab2 > objkey-uuid-ab2.rslt&
nohup mysql test < objkey-uuid-ac2 > objkey-uuid-ac2.rslt&
nohup mysql test < objkey-uuid-ad2 > objkey-uuid-ad2.rslt&
nohup mysql test < objkey-uuid-ae2 > objkey-uuid-ae2.rslt&
nohup mysql test < objkey-uuid-af2 > objkey-uuid-af2.rslt&

