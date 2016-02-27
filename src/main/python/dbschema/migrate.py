#!/usr/bin/python2

"""Apply a sequence of migration scripts to an Ambra database.

This program is a replacement for the "bootstrap migration" feature of the old
Ambra stack, where the webapp would automatically migrate its own database on
start-up if it was out of date. It is intended to have the old system stand
alone from the Ambra webapp, so that the Rhino+Wombat stack can be updated
without a running Ambra 2.* webapp.

The Ambra database reports its own schema version in the `version` table. There
are SQL scripts in the `migrations/` directory associated with particular
schema version numbers. This program reads the database's version, applies the
SQL scripts needed to bring the schema up to date, and inserts new version rows
to record the migration.

The `version` table has an `updateInProcess` column (which would be better
called "update in progress") which is meant to prevent concurrent migration
attempts. It was more relevant when the migrations were run automatically. A
`version` row is inserted with the `updateInProcess` bit set just before
beginning a migration, then it clears the bit when done. The bit being set
blocks other migrations from starting. Ambra never had error-handling or
rollbacks if the migration scripts encountered an error, and we haven't added
one here. A migration failure leaves the bit set indefinitely, giving the false
appearance that the migration is running indefinitely. The only way to fix this
in Ambra was to manually clear the bit; here, the workflow is the same, but
this program has a `--force_clear` option to clear it.
"""

from __future__ import print_function
import MySQLdb  # may require `apt-get install python-mysqldb`
import argparse
import json

log = print

def parse_database_args():
  """Parse arguments from the command line."""
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
  """Read file contents to a string."""
  with open(filename) as f:
    return f.read()


class DatabaseClient(object):
  """A database connection and the set of arguments to produce it."""

  def __init__(self, cmd_line_args):
    self.args = cmd_line_args
    self.con = MySQLdb.connect(host=self.args.dbHost, user=self.args.dbUser,
                               passwd=self.args.dbPass, db=self.args.dbName,
                               charset='utf8', use_unicode=True)

  def _execute(self, query, params):
    try:
      cursor = self.con.cursor()
      cursor.execute(query, params)
      return cursor.fetchall()
    finally:
      cursor.close()

  def read(self, query, *params):
    """Execute a read-only query and return the result rows."""
    return self._execute(query, params)

  def write(self, query, *params):
    """Execute a destructive query and return the result rows."""
    return self._execute(query, params)


class Migration(object):
  """The migration to update to a schema version number.

  Each instance represents the steps to increment up to a particular schema
  version from the previous version. It encapsulates a sequence of one or
  more SQL script files.
  """

  def __init__(self, number, script_names):
    self.number = int(number)
    self.script_names = tuple(str(sn) for sn in script_names)

  def __repr__(self):
    return 'Migration({0!r}, {1!r})'.format(self.number, self.script_names)

  def get_number(self):
    """Return the target schema version number."""
    return self.number

  def start_progress(self, db_client):
    """Insert a version row recording the start of the migration."""
    log('Beginning {0}'.format(self.number))
    name = 'Schema {0}'.format(self.number)
    db_client.write(('INSERT INTO version (name, version, updateInProcess, '
                     'created, lastModified) '
                     'VALUES (%s, %s, 1, now(), now())'),
                    name, self.number)

  def record_success(self, db_client):
    """Update a version row to show that the migration is done."""
    log('Completed {0}'.format(self.number))
    db_client.write(('UPDATE version SET updateInProcess = 0, '
                     'lastModified = now() '
                     'WHERE version = %s'),
                    self.number)

  def apply(self, db_client):
    """Execute the schema migration using the given database client.

    This should be called only when the connected database already has the
    immediately previous schema version.
    """
    scripts = [(fn, read_file_to_string('./migrations/' + fn))
               for fn in self.script_names]
    self.start_progress(db_client)
    for (script_name, script) in scripts:
      log('  Beginning {0}'.format(script_name))
      result = db_client.write(script)
      log('  Completed {0}'.format(script_name))
    self.record_success(db_client)


class MigrationTable(object):
  """The set of all available schema migrations."""

  def __init__(self, input):
    def parse_migrations():
      """Parse JSON associating migration scripts to schema version numbers."""
      for obj in input:
        number = obj['number']
        scripts = obj['scripts']
        if isinstance(scripts, str):
          scripts = [scripts]
        yield Migration(number, scripts)

    self.migrations = sorted(parse_migrations(), key=Migration.get_number)

  def get_migrations(self, start_version, target_version=None):
    """Get all migrations in an interval between version numbers.

    If `target_version` is None, get migrations up to the latest one.
    Returns Migration objects sorted by version number.
    """

    def is_in_interval(migration):
      version = migration.number
      return ((version > start_version) and
              ((target_version is None) or (version <= target_version)))

    return [m for m in self.migrations if is_in_interval(m)]


def force_clear(db_client):
  """Clear all in-progress bits.

  This should be used to clear the bits from previous migration runs that
  halted abnormally, and only when another migration is not running
  concurrently.
  """
  db_client.write('UPDATE version SET updateInProcess = 0;')

MIGRATIONS_IN_PROGRESS_MESSAGE = """
Migrations are marked as in progress for: {versions}

This could be due to a concurrent migration process or a previous migration
that was interrupted. Please check that no other migration processes are
running and use the --force_clear option to proceed.
"""

def run_migrations(db_client):
  """Bring the connected database's schema up to date."""

  in_progress = db_client.read(
    'SELECT version FROM version WHERE updateInProcess > 0;')
  if in_progress:
    raise Exception(MIGRATIONS_IN_PROGRESS_MESSAGE.format(
      versions=[int(r[0]) for r in in_progress]))

  version = db_client.read('SELECT MAX(version) FROM version;')[0][0]

  with open('migrations.json') as f:
    mt = MigrationTable(json.load(f))

  for m in mt.get_migrations(version):
    m.apply(db_client)

args = parse_database_args()
db_client = DatabaseClient(args)
if args.force_clear:
  force_clear(db_client)
run_migrations(db_client)
