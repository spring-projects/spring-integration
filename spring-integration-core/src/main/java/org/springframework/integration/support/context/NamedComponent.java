/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
