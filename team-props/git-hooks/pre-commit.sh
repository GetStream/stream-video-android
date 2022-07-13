#!/bin/sh

echo "Running code formatting with spotless..."

./gradlew spotlessApply

status=$?

if [ "$status" = 0 ] ; then
    echo "Code formatting success."
    exit 0
else
    echo 1>&2 "Static analysis found violations it could not fix."
    exit 1
fi