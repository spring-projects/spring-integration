/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
