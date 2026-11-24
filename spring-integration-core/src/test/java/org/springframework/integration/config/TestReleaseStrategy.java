/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config;

import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.store.MessageGroup;

/**
 * @author Marius Bogoevici
 */
public class TestReleaseStrategy implements ReleaseStrategy {

	public boolean canRelease(MessageGroup messages) {
		throw new UnsupportedOperationException("This is not intended to be implemented, but to verify injection into an <aggregator>");
	}

}
