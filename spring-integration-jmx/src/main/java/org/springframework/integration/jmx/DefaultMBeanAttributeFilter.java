/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.jmx;

import javax.management.ObjectName;

/**
 * @author Stuart Williams
 * @since 3.0
 *
 */
public class DefaultMBeanAttributeFilter implements MBeanAttributeFilter {

	@Override
	public boolean accept(ObjectName objectName, String attributeName) {
		return true;
	}

}
