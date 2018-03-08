#!/usr/bin/env bash
export PATH="$HOME/bin:$HOME/.pyenv/bin:$PATH"
eval "$(pyenv init -)"
eval "$(pyenv virtualenv-init -)"

SCRIPT_DIR=%teamcity.build.workingDir%

cd $SCRIPT_DIR
rm test/Base/*.pyc
rm test/api/*.pyc
rm test/api/RequestObject/*.pyc

pyenv activate raro3
pyenv version

python -m test.api.test_articlecc
python -m test.api.test_articlelistcc
python -m test.api.test_configuration
python -m test.api.test_ingestiblec
python -m test.api.test_journalcc
python -m test.api.test_zip_ingestion

pyenv deactivate
pyenv version
