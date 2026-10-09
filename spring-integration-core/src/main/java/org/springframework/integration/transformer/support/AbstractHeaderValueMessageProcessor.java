/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.transformer.support;

import org.jspecify.annotations.Nullable;

/**
 * @param <T> inbound payload type.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 3.0
 */
public abstract class AbstractHeaderValueMessageProcessor<T extends @Nullable Object> implements HeaderValueMessageProcessor<T> {

	private @Nullable Boolean overwrite = null;

	public void setOverwrite(@Nullable Boolean overwrite) {
		this.overwrite = overwrite;
	}

	@Override
	public @Nullable Boolean isOverwrite() {
		return this.overwrite;
	}

}
