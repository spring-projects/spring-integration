/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.json;

import org.springframework.integration.support.json.Jackson2JsonMessageParser;
import org.springframework.integration.support.json.JsonInboundMessageMapper.JsonMessageParser;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class Jackson2JsonSymmetricalMessageMappingTests extends AbstractJsonSymmetricalMessageMappingTests {

	@Override
	protected JsonMessageParser<?> getParser() {
		return new Jackson2JsonMessageParser();
	}

}
