/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.jmx;

/**
 * A {@link org.springframework.integration.core.MessageSource} implementation that
 * retrieves a snapshot of a filtered subset of the MBean tree.
 *
 * @author Stuart Williams
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 * @deprecated since 7.0 in favor {@link org.springframework.integration.jmx.inbound.MBeanTreePollingMessageSource}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class MBeanTreePollingMessageSource
		extends org.springframework.integration.jmx.inbound.MBeanTreePollingMessageSource {

	/**
	 * @param converter The converter.
	 */
	public MBeanTreePollingMessageSource(org.springframework.integration.jmx.inbound.MBeanObjectConverter converter) {
		super(converter);
	}

}
