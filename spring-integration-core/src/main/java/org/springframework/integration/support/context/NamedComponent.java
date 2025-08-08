/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.support.context;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public interface NamedComponent {

	String getComponentName();

	String getComponentType();

	default String getBeanName() {
		return getComponentName();
	}

}
