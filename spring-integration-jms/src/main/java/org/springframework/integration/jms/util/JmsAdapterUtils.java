/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.jms.util;

import org.springframework.util.StringUtils;

/**
 * @author Liujiong
 * @author Gary Russell
 * @since 4.1
 *
 */
public abstract class JmsAdapterUtils {

	public static final String AUTO_ACKNOWLEDGE_STRING = "auto";

	public static final String DUPS_OK_ACKNOWLEDGE_STRING = "dups-ok";

	public static final String CLIENT_ACKNOWLEDGE_STRING = "client";

	public static final String SESSION_TRANSACTED_STRING = "transacted";

	public static final int SESSION_TRANSACTED = 0;

	public static final int AUTO_ACKNOWLEDGE = 1;

	public static final int CLIENT_ACKNOWLEDGE = 2;

	public static final int DUPS_OK_ACKNOWLEDGE = 3;

	public static Integer parseAcknowledgeMode(String acknowledge) {
		if (StringUtils.hasText(acknowledge)) {
			int acknowledgeMode = AUTO_ACKNOWLEDGE;
			if (SESSION_TRANSACTED_STRING.equals(acknowledge)) {
				acknowledgeMode = SESSION_TRANSACTED;
			}
			else if (DUPS_OK_ACKNOWLEDGE_STRING.equals(acknowledge)) {
				acknowledgeMode = DUPS_OK_ACKNOWLEDGE;
			}
			else if (CLIENT_ACKNOWLEDGE_STRING.equals(acknowledge)) {
				acknowledgeMode = CLIENT_ACKNOWLEDGE;
			}
			else if (!AUTO_ACKNOWLEDGE_STRING.equals(acknowledge)) {
				throw new IllegalStateException("Invalid JMS 'acknowledge' setting: " +
						"only \"auto\", \"client\", \"dups-ok\" and \"transacted\" supported.");
			}
			return acknowledgeMode;
		}
		else {
			return null;
		}
	}

}
