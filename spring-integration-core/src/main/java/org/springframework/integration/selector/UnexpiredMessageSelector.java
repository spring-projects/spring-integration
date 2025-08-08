/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.selector;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;

/**
 * A {@link MessageSelector} that accepts {@link Message Messages} that are
 * <em>not</em> yet expired. If a Message's expiration date header is
 * <code>null</code>, that Message <em>never</em> expires.
 *
 * @author Mark Fisher
 */
public class UnexpiredMessageSelector implements MessageSelector {

	public boolean accept(Message<?> message) {
		Long expirationDate = new IntegrationMessageHeaderAccessor(message).getExpirationDate();
		if (expirationDate == null) {
			return true;
		}
		return expirationDate > System.currentTimeMillis();
	}

}
