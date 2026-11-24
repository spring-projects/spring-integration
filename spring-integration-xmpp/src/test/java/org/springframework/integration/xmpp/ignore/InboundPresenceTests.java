/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.xmpp.ignore;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * this class demonstrates that when I launch this and then manipulate the status of the
 * user using Pidgin, the updated state is immediately delivered to the Spring Integration bus.
 *
 * @author Josh Long
 * @since 2.0
 */
@SpringJUnitConfig
@Disabled
public class InboundPresenceTests {

	@Test
	public void run() throws Exception {
		Thread.sleep(60 * 1000 * 1000);
	}

}
