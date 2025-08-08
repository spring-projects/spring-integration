/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.source;

import javax.xml.transform.Source;

/**
 * Factory to create a {@link Source} possibly taking into account
 * the provided message payload instance.
 *
 * @author Jonas Partner
 */
public interface SourceFactory {

	/**
	 * Create appropriate {@link Source} instance for {@code payload}
	 *
	 * @param payload The payload.
	 * @return The source.
	 */
	Source createSource(Object payload);

}
