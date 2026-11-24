/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
