import re
import string

"""Code generation for the article ingestion testing framework.

One half of the software that is meant to produce test cases automatically
for article ingestion. The other half is an ad hoc extension to Ambra Admin
that generate further Java code. The Java code generated there will call
the methods generated here, to produce the expected values for test cases.

To run:
    python3 types.py

This is riddled with kludges and well below my usual standards for writing
Python. Sorry. Hopefully the output will be need to be modified only
rarely, and such modifications will be simple enough that you can just edit
the Java code instead of this script.

But I would like to modify it to print the classes to their own files
instead of just printing everything to stdout. The output works as nested
classes of IngestionTestCase, but I've manually split the checked-in
versions into their own classes.
"""

def cap(s):
    return s[:1].upper() + s[1:]
def low(s):
    return s[:1].lower() + s[1:]

PRIMITIVES = {
    'byte': 'Byte',
    'short': 'Short',
    'int': 'Integer',
    'long': 'Long',
    'float': 'Float',
    'double': 'Double',
    'boolean': 'Boolean',
    'char': 'Character',
    }
def is_primitive(type_name):
    return type_name in PRIMITIVES

GETTER_SPECIAL_CASES = {
    'eLocationId': 'geteLocationId',
    'eLocationID': 'geteLocationID',
    'eIssn': 'geteIssn',
    }

def subclass_for(java_type, field_decls, out=None):
    generated_type = 'Expected' + java_type

    # Class declaration
    print('public class {0} extends ExpectedEntity<{1}> {{'
          .format(generated_type, java_type))

    # Field declarations
    for (t, n) in field_decls:
        print('private final {t} {n};'.format(t=t, n=n))

    # Constructor
    print('private {0}(Builder builder) {{ super({1}.class);'
          .format(generated_type, java_type))
    for (t, n) in field_decls:
        print('this.{n} = builder.{n};'.format(t=t, n=n))
    print('}')
    print('public static Builder builder() { return new Builder(); }')

    # The testing method
    print('@Override public Collection<AssertionFailure<?>> test({t} {n}) {{'
          .format(t=java_type, n=low(java_type)))
    print('Collection<AssertionFailure<?>> failures = Lists.newArrayList();')
    for (t, n) in field_decls:
        getter = GETTER_SPECIAL_CASES.get(n, 'get' + cap(n))
        print('testField(failures, "{n}", {e}.{g}(), {n});'
              .format(n=n, e=low(java_type), g=getter))
    print('return ImmutableList.copyOf(failures); }')

    # Builder subclass
    print('public static class Builder {')
    for (t, n) in field_decls:
        print('private {t} {n};'.format(t=t, n=n))
    for (t, n) in field_decls:
        print('public Builder set{c}({t} {n}) {{ this.{n} = {n}; return this; }}'
              .format(t=t, n=n, c=cap(n)))
    print('public {t} build() {{ return new {t}(this); }}'
          .format(t=generated_type))
    print('}')

    # Just in case
    print('@Override public boolean equals(Object obj) {')
    print('if (obj == this) return true;')
    print('if (obj == null || obj.getClass() != getClass()) return false;')
    print('{t} that = ({t}) obj;'.format(t=generated_type))
    for (t, n) in field_decls:
        # Autoboxing will take care of this if t is primitive.
        # A little wasteful, but who cares.
        print('if (!Objects.equal(this.{n}, that.{n})) return false;'
              .format(n=n))
    print('return true; }')
    print('@Override public int hashCode() {')
    print('final int prime = 31; int hash = 1;')
    for (t, n) in field_decls:
        print('hash = prime * hash + ObjectUtils.hashCode({0});'.format(n))
        # if t in PRIMITIVES:
        #     hash_expr = ('{t}.valueOf({n}).hashCode()'
        #                  .format(t=PRIMITIVES[t], n=n))
        # else:
        #     hash_expr = ('({n} == null ? 0 : {n}.hashCode())'.format(n=n))
        # print('hash = prime * hash + {0};'.format(hash_expr))
    print('return hash; }')

    # Close the class declaration
    print('}')
