/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.support.management;

import org.springframework.integration.support.context.NamedComponent;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
@IntegrationManagedResource
public interface TrackableComponent extends NamedComponent {

	@ManagedOperation
	void setShouldTrack(boolean shouldTrack);

}
