/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.json;

import org.springframework.integration.support.json.JacksonJsonMessageParser;
import org.springframework.integration.support.json.JsonInboundMessageMapper.JsonMessageParser;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class Jackson2JsonSymmetricalMessageMappingTests extends AbstractJsonSymmetricalMessageMappingTests {

	@Override
	protected JsonMessageParser<?> getParser() {
		return new JacksonJsonMessageParser();
	}

}
