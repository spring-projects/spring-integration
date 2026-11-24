/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.support;

import java.util.Properties;

/**
 * A {@code Builder} pattern implementation for the {@link Properties}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 */
public class PropertiesBuilder {

	private final Properties properties = new Properties();

	public PropertiesBuilder put(Object key, Object value) {
		this.properties.put(key, value);
		return this;
	}

	public Properties get() {
		return this.properties;
	}

}
