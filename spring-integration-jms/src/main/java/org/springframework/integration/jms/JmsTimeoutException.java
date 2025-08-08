/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.jms;

import java.io.Serial;

import org.springframework.jms.JmsException;

/**
 * A timeout occurred within an async gateway.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class JmsTimeoutException extends JmsException {

	@Serial
	private static final long serialVersionUID = 5439915454935047936L;

	public JmsTimeoutException(String description) {
		super(description);
	}

}
