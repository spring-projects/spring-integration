/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * @author Artem Bilan
 * @since 2.2
 */

public interface DelayHandlerManagement {

	@ManagedAttribute
	int getDelayedMessageCount();

	@ManagedOperation
	void reschedulePersistedMessages();

}
