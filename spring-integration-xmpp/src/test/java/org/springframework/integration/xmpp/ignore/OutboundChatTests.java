/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.xmpp.ignore;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Testing support for sending messages.
 *
 * @author Josh Long
 * @since 2.0
 */
@SpringJUnitConfig
@Disabled
public class OutboundChatTests {

	@Test
	public void run() throws Exception {
		Thread.sleep(10 * 1000 * 100);
	}

}
