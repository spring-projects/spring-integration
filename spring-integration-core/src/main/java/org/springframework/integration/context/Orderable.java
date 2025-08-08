/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.context;

import org.springframework.core.Ordered;

/**
 * Interface that extends {@link Ordered} while also exposing the
 * {@link #setOrder(int)} as an interface-level so that it is avaiable
 * on AOP proxies, etc.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public interface Orderable extends Ordered {

	/**
	 * Set the order for this component.
	 *
	 * @param order the order.
	 */
	void setOrder(int order);

}
