/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.config;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 */
public class PresenceInboundChannelAdapterParserTests {

	@Test
	public void testPresenceInboundChannelAdapterParser() {
		assertThatNoException()
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("PresenceInboundChannelAdapterParserTests-context.xml",
								this.getClass())
								.close());
	}

}
