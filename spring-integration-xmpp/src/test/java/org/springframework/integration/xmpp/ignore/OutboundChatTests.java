/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
