/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class TestSelector implements MessageSelector {

	private final boolean accept;

	public TestSelector(boolean accept) {
		this.accept = accept;
	}

	public boolean accept(Message<?> message) {
		return this.accept;
	}

}
