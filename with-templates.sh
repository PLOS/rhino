#!/bin/sh
# Update files named FOO.template.whatever or FOO.whatever.template
# with environment variable substitution.
for template in ${TEMPLATES}; do
    envsubst < "$template" > "$(printf '%s' "$template" | sed -e 's/\(\.template$\|template\.\)//')"
done
exec "$@"
