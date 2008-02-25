#!/bin/sh

JARS=`find ../dist ../lib -iname *.jar`
CP=`echo $JARS | sed 's/ /:/g'`

java -cp $CP org.springframework.integration.samples.cafe.CafeDemo $*