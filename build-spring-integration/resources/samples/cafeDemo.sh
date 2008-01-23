#!/bin/sh

java -cp ../dist/spring-integration-core-1.0.0.m1.jar:\
../dist/spring-integration-samples-1.0.0.m1.jar:\
../lib/aopalliance-1.0.jar:\
../lib/commons-logging-1.1.jar:\
../lib/spring-aop-2.5.0.jar:\
../lib/spring-beans-2.5.0.jar:\
../lib/spring-context-2.5.0.jar:\
../lib/spring-context-support-2.5.0.jar:\
../lib/spring-core-2.5.0.jar\
 org.springframework.integration.samples.cafe.CafeDemo $*
