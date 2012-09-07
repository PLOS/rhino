#!/bin/sh
testdata=../resources/data
./admintest.py                              \
    10.1371/journal.pone.0038869            \
    $testdata/journal.pone.0038869.xml      \
    10.1371/journal.pone.0038869.g002       \
    $testdata/journal.pone.0038869.g002.tif
