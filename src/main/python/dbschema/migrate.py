#!/usr/bin/python2

from __future__ import print_function
import MySQLdb  # may require `apt-get install python-mysqldb`
import argparse
import itertools
import json

log = print

def parse_database_args():
  parser = argparse.ArgumentParser(description='Update blank display names.')
  parser.add_argument('--dbUser', default='root',
                      help='MySQL user (default: root)')
  parser.add_argument('--dbPass', default='',
                      help='MySQL password (no default)')
  parser.add_argument('--dbName', default='ambra',
                      help='MySQL database (default: ambra)')
  parser.add_argument('--dbHost', default='localhost',
                      help='MySQL host (default: localhost)')
  parser.add_argument('--dbPort', type=int, default='3307',
                      help='port to use with MySQL host (default: 3307)')
  parser.add_argument('--force_clear', action='store_true',
                      help='Clear migrations in progress')

  return parser.parse_args()

def read_file_to_string(filename):
  with open(filename) as f:
    return f.read()


class DatabaseClient(object):
  def __init__(self, cmd_line_args):
    self.args = cmd_line_args
    self.con = MySQLdb.connect(host=self.args.dbHost, user=self.args.dbUser,
                               passwd=self.args.dbPass, db=self.args.dbName,
                               charset='utf8', use_unicode=True)

  def _execute(self, query, is_destructive):
    try:
      cursor = self.con.cursor()
      cursor.execute(query)
      if is_destructive:
        self.con.commit()
      return cursor.fetchall()
    finally:
      cursor.close()

  def read(self, query):
    return self._execute(query, False)

  def write(self, query):
    return self._execute(query, True)


class Migration(object):
  def __init__(self, number, script_names):
    self.number = int(number)
    self.script_names = tuple(str(sn) for sn in script_names)

  def __repr__(self):
    return 'Migration({0!r}, {1!r})'.format(self.number, self.script_names)

  def get_number(self):
    return self.number

  def start_progress(self, db_client):
    log('Beginning {0}'.format(self.number))
    name = 'Schema {0}'.format(self.number)
    stmt = ('INSERT INTO version (name, version, updateInProcess, '
            'created, lastModified) '
            'VALUES ({0!r}, {1!r}, 1, now(), now())') \
      .format(name, self.number)
    db_client.write(stmt)

  def record_success(self, db_client):
    log('Completed {0}'.format(self.number))
    stmt = ('UPDATE version SET updateInProcess = 0, lastModified = now() '
            'WHERE version = {0}') \
      .format(self.number)
    db_client.write(stmt)

  def apply(self, db_client):
    scripts = [(fn, read_file_to_string('./migrations/' + fn))
               for fn in self.script_names]
    self.start_progress(db_client)
    for (script_name, script) in scripts:
      log('  Beginning {0}'.format(script_name))
      result = db_client.write(script)
      log('  Completed {0}'.format(script_name))
    self.record_success(db_client)


class MigrationTable(object):
  def __init__(self, input):
    def parse_migrations():
      for obj in input:
        number = obj['number']
        scripts = obj['scripts']
        if isinstance(scripts, str):
          scripts = [scripts]
        yield Migration(number, scripts)

    self.migrations = sorted(parse_migrations(), key=Migration.get_number)

  def get_migrations(self, start_version, target_version=None):
    def is_in_interval(migration):
      version = migration.number
      return ((version > start_version) and
              ((target_version is None) or (version <= target_version)))
    return [m for m in self.migrations if is_in_interval(m)]


def force_clear(db_client):
  db_client.write('UPDATE version SET updateInProcess = 0;')

def run_migrations(db_client):
  in_progress = db_client.read(
    'SELECT version FROM version WHERE updateInProcess > 0;')
  if in_progress:
    msg = 'Migrations are marked in progress for version{0}: {1}'.format(
      ('' if len(in_progress) == 1 else 's'),
      [int(r[0]) for r in in_progress])
    raise Exception(msg)

  version = db_client.read('SELECT MAX(version) FROM version;')[0][0]

  with open('migrations.json') as f:
    mt = MigrationTable(json.load(f))

  for m in mt.get_migrations(version):
    m.apply(db_client)

args = parse_database_args()
db_client = DatabaseClient(args)
if args.force_clear:
  force_clear(db_client)
else:
  run_migrations(db_client)
