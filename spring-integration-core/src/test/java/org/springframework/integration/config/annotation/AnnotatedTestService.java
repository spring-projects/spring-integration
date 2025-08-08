/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.annotation;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.endpoint.annotation.TestService;

/**
 * @author Mark Fisher
 */
@MessageEndpoint
public class AnnotatedTestService implements TestService {

	@ServiceActivator(inputChannel = "inputChannel", outputChannel = "outputChannel")
	public String sayHello(String name) {
		return "hello " + name;
	}

}
