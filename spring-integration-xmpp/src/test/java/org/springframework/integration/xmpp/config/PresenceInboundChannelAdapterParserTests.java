/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
