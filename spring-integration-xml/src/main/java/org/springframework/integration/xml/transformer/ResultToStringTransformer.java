/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.xml.transformer;

import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.TransformerFactoryUtils;

/**
 * Converts the passed {@link Result} to an instance of {@link String}.
 * Supports {@link StringResult} and {@link DOMResult}
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Christian Tzolov
 */
public class ResultToStringTransformer implements ResultTransformer {

	private final Lock lock = new ReentrantLock();

	private final TransformerFactory transformerFactory;

	private Properties outputProperties;

	public ResultToStringTransformer() {
		this.transformerFactory = TransformerFactoryUtils.newInstance();
	}

	/**
	 * Construct an instance based on the provided {@link TransformerFactory}.
	 * @param transformerFactory the {@link TransformerFactory} to use.
	 * @since 4.3.19
	 */
	public ResultToStringTransformer(TransformerFactory transformerFactory) {
		Assert.notNull(transformerFactory, "'transformerFactory' must not be null.");
		this.transformerFactory = transformerFactory;
	}

	public void setOutputProperties(Properties outputProperties) {
		this.outputProperties = outputProperties;
	}

	public Object transformResult(Result result) {
		String returnString = null;
		if (result instanceof StringResult) {
			returnString = result.toString();
		}
		else if (result instanceof DOMResult) {
			try {
				StringResult stringResult = new StringResult();
				getNewTransformer().transform(
						new DOMSource(((DOMResult) result).getNode()), stringResult);
				returnString = stringResult.toString();
			}
			catch (TransformerException e) {
				throw new MessagingException("failed to transform from DOMSource failed", e);
			}
		}
		if (returnString == null) {
			throw new MessagingException("failed to convert Result type ["
					+ result.getClass().getName() + "] to string");
		}
		return returnString;
	}

	private Transformer getNewTransformer() throws TransformerConfigurationException {
		Transformer transformer;
		this.lock.lock();
		try {
			transformer = this.transformerFactory.newTransformer();
		}
		finally {
			this.lock.unlock();
		}
		if (this.outputProperties != null) {
			transformer.setOutputProperties(this.outputProperties);
		}
		return transformer;
	}

}
