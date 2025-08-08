/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class TestSource implements MessageSource<String> {

	public Message<String> receive() {
		return new GenericMessage<>("test");
	}

}
