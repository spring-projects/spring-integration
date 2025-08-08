/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.config.annotation;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.endpoint.annotation.TestService;

/**
 * @author Gary Russell
 */
@MessageEndpoint
public class AnnotatedTestServiceWithAdvice implements TestService {

	@ServiceActivator(inputChannel = "inputChannel", outputChannel = "outputChannel")
	public String sayHello(String name) {
		return "hello " + name;
	}

	@ServiceActivator(inputChannel = "advisedIn", outputChannel = "advisedOut", adviceChain = "advice")
	public String sayHelloWithAdvice(String name) {
		return "hello " + name;
	}

}
