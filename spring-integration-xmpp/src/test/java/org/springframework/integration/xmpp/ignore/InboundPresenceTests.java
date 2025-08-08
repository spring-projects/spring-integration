/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
