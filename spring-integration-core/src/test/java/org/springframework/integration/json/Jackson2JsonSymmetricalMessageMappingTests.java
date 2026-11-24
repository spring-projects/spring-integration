/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
