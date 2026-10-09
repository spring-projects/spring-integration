/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.jmx;

/**
 * @author Stuart Williams
 *
 * @since 3.0
 *
 * @deprecated since 7.0 in favor {@link org.springframework.integration.jmx.inbound.MBeanAttributeFilter}
 */
@Deprecated(forRemoval = true, since = "7.0")
@FunctionalInterface
public interface MBeanAttributeFilter extends org.springframework.integration.jmx.inbound.MBeanAttributeFilter {

}
