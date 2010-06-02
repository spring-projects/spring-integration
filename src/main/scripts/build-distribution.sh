#!/bin/sh
cd `dirname $0`
mvn -f ../../../pom.xml -Dmaven.test.skip=true clean package javadoc:aggregate docbkx:generate-pdf docbkx:generate-html assembly:assembly
cd -
