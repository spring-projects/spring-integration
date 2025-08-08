/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.annotation;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;

/**
 * @author Mark Fisher
 */
@MessageEndpoint
public class TypeConvertingTestEndpoint {

	@ServiceActivator(inputChannel = "inputChannel", outputChannel = "outputChannel")
	public int multiplyByTwo(int number) {
		return number * 2;
	}

}
