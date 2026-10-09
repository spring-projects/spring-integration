/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.transformer.support;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.handler.MessageProcessor;

/**
 * @param <T> the payload type.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 3.0
 */
public interface HeaderValueMessageProcessor<T extends @Nullable Object> extends MessageProcessor<T> {

	/**
	 * Return the overwrite flag.
	 * If null, the default overwrite flag is used from the enricher.
	 * @return the overwrite flag
	 */
	@Nullable
	Boolean isOverwrite();

}
