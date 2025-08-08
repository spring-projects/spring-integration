/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router.config;

import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class TestSplitterImpl extends AbstractMessageSplitter {

	@Override
	protected Object splitMessage(Message<?> message) {
		return message.getPayload().toString().split("\\.");
	}

}
