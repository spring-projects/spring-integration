/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.support.management.metrics;

import org.jspecify.annotations.Nullable;

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
