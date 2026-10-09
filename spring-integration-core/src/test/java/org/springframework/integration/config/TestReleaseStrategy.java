/*
 * Copyright 2002-present the original author or authors.
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
