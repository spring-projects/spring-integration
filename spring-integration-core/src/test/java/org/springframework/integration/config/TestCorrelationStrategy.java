/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config;

import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.messaging.Message;

/**
 * @author Marius Bogoevici
 */
public class TestCorrelationStrategy implements CorrelationStrategy {

	public Object getCorrelationKey(Message<?> message) {
		throw new UnsupportedOperationException("for configuration test only");
	}

}
