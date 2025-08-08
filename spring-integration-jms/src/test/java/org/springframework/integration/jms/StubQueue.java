/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import jakarta.jms.JMSException;
import jakarta.jms.Queue;

/**
 * @author Mark Fisher
 */
public class StubQueue implements Queue {

	public String getQueueName() throws JMSException {
		return null;
	}

}
