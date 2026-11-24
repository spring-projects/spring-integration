/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.acks;

/**
 * A factory for creating {@link AcknowledgmentCallback}s.
 *
 * @param <T> a type containing information with which to populate the acknowledgment.
 *
 * @author Gary Russell
 *
 * @since 5.0.1
 *
 */
@FunctionalInterface
public interface AcknowledgmentCallbackFactory<T> {

	/**
	 * Create the callback.
	 * @param info information for the callback to process the acknowledgment.
	 * @return the callback
	 */
	AcknowledgmentCallback createCallback(T info);

}
