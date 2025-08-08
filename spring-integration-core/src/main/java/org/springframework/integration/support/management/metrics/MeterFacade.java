/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.support.management.metrics;

import org.springframework.lang.Nullable;

/**
 * Facade for Meters.
 *
 * @author Gary Russell
 * @since 5.1
 *
 */
public interface MeterFacade {

	/**
	 * Remove this meter facade.
	 * @param <T> the type of meter removed.
	 * @return the facade that was removed, or null.
	 */
	@Nullable
	default <T extends MeterFacade> T remove() {
		return null;
	}

}
