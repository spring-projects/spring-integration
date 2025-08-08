/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.transformer.support;

/**
 * @param <T> inbound payload type.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 3.0
 */
public abstract class AbstractHeaderValueMessageProcessor<T> implements HeaderValueMessageProcessor<T> {

	// null indicates no explicit setting
	private Boolean overwrite = null;

	public void setOverwrite(Boolean overwrite) {
		this.overwrite = overwrite;
	}

	@Override
	public Boolean isOverwrite() {
		return this.overwrite;
	}

}
