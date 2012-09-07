#!/bin/sh

testdata=../resources/data
doi=journal.pone.0038869

./admintest.py              \
    10.1371/$doi.xml        \
    $testdata/$doi.xml      \
    10.1371/$doi.g002.tif   \
    $testdata/$doi.g002.tif
