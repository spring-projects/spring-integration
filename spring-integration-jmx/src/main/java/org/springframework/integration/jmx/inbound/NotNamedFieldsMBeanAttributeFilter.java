/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.jmx.inbound;

import javax.management.ObjectName;

/**
 * @author Stuart Williams
 * @author Artem Bilan
 *
 * @since 7.0
 *
 */
public class NotNamedFieldsMBeanAttributeFilter extends NamedFieldsMBeanAttributeFilter {

	/**
	 * @param namedFields The named fields that should be filtered.
	 */
	public NotNamedFieldsMBeanAttributeFilter(String... namedFields) {
		super(namedFields);
	}

	@Override
	public boolean accept(ObjectName objectName, String attributeName) {
		return !super.accept(objectName, attributeName);
	}

}
