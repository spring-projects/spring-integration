/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xml;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The {@link TransformerFactory} configuration wrapper with some common settings.
 * Copy of {@link org.springframework.xml.transform.TransformerFactoryUtils}, but with
 * catching exception for unknown properties.
 *
 * @author Greg Turnquist
 * @author Artem Bilan
 *
 * @since 4.3.21
 */

public final class TransformerFactoryUtils {

	private static final Log LOG = LogFactory.getLog(TransformerFactoryUtils.class);

	/**
	 * Build a new {@link TransformerFactory} using the default constructor.
	 */
	public static TransformerFactory newInstance() {
		return defaultSettings(TransformerFactory.newInstance());
	}

	/**
	 * Build an {@link TransformerFactory} and prevent external entities from accessing.
	 * @see TransformerFactory#newInstance()
	 */
	public static TransformerFactory newInstance(Class<? extends TransformerFactory> transformerFactoryClass) {
		try {
			return defaultSettings(transformerFactoryClass.newInstance());
		}
		catch (InstantiationException e) {
			throw new TransformerFactoryConfigurationError(e,
					"Could not instantiate TransformerFactory [" + transformerFactoryClass + "]");
		}
		catch (IllegalAccessException e) {
			throw new TransformerFactoryConfigurationError(e,
					"Could not instantiate TransformerFactory [" + transformerFactoryClass + "]");
		}
	}

	/**
	 * Prevent external entities from accessing.
	 */
	private static TransformerFactory defaultSettings(TransformerFactory factory) {
		try {
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		}
		catch (IllegalArgumentException e) {
			if (LOG.isWarnEnabled()) {
				LOG.warn(XMLConstants.ACCESS_EXTERNAL_DTD + " property not supported by " +
						factory.getClass().getCanonicalName());
			}
		}

		try {
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		}
		catch (IllegalArgumentException e) {
			if (LOG.isWarnEnabled()) {
				LOG.warn(XMLConstants.ACCESS_EXTERNAL_STYLESHEET + " property not supported by " +
						factory.getClass().getCanonicalName());
			}
		}

		return factory;
	}

	private TransformerFactoryUtils() {
	}

}
