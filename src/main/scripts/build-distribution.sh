#!/bin/sh
mvn -Dmaven.test.skip=true clean package javadoc:aggregate docbkx:generate-pdf docbkx:generate-html assembly:assembly
