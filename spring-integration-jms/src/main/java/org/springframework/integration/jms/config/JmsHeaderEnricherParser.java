/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.config;

import org.springframework.integration.config.xml.HeaderEnricherParserSupport;
import org.springframework.jms.support.JmsHeaders;

/**
 * Header enricher for JMS specific values.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class JmsHeaderEnricherParser extends HeaderEnricherParserSupport {

	@SuppressWarnings("this-escape")
	public JmsHeaderEnricherParser() {
		this.addElementToHeaderMapping("correlation-id", JmsHeaders.CORRELATION_ID);
		this.addElementToHeaderMapping("reply-to", JmsHeaders.REPLY_TO);
	}

}
