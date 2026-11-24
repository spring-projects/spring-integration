/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
