/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xml.transformer;

import java.util.Properties;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.springframework.messaging.MessagingException;
import org.springframework.xml.transform.StringResult;

/**
 * Converts the passed {@link Result} to an instance of {@link String}.
 * Supports {@link StringResult} and {@link DOMResult}
 *
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class ResultToStringTransformer implements ResultTransformer {

	private volatile Properties outputProperties;

	private final TransformerFactory transformerFactory;


	public ResultToStringTransformer() {
		this.transformerFactory = TransformerFactory.newInstance();
	}


	public void setOutputProperties(Properties outputProperties) {
		this.outputProperties = outputProperties;
	}

	public Object transformResult(Result result) {
		String returnString = null;
		if (result instanceof StringResult) {
			returnString = ((StringResult) result).toString();
		}
		else if (result instanceof DOMResult) {
			try {
				StringResult stringResult = new StringResult();
				this.getNewTransformer().transform(
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
		Transformer transformer = null;
		synchronized (this.transformerFactory) {
			transformer = this.transformerFactory.newTransformer();
		}
		if (this.outputProperties != null) {
			transformer.setOutputProperties(this.outputProperties);
		}
		return transformer;
	}

}
