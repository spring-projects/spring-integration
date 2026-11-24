/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.jmx;

/**
 * @author Stuart Williams
 * @author Artem Bilan
 *
 * @since 3.0
 *
 * @deprecated since 7.0 in favor {@link org.springframework.integration.jmx.inbound.NamedFieldsMBeanAttributeFilter}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class NamedFieldsMBeanAttributeFilter
		extends org.springframework.integration.jmx.inbound.NamedFieldsMBeanAttributeFilter {

	/**
	 * @param namedFields The named fields that should pass the filter.
	 */
	public NamedFieldsMBeanAttributeFilter(String... namedFields) {
		super(namedFields);
	}

}
