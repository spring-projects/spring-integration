/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.support.management;

import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * Message sources implementing this interface have additional properties that
 * can be set or examined using JMX.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
@IntegrationManagedResource
public interface MessageSourceManagement {

	/**
	 * Set the maximum number of objects the source should fetch if it is necessary to
	 * fetch objects. Setting the
	 * maxFetchSize to 0 disables remote fetching, a negative value indicates no limit.
	 * @param maxFetchSize the max fetch size; a negative value means unlimited.
	 */
	@ManagedAttribute(description = "Maximum objects to fetch")
	void setMaxFetchSize(int maxFetchSize);

	/**
	 * Return the max fetch size.
	 * @return the max fetch size.
	 * @see #setMaxFetchSize(int)
	 */
	@ManagedAttribute
	int getMaxFetchSize();

}
