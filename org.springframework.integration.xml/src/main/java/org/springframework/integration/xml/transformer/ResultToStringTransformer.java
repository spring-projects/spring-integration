/*
 * Copyright 2002-2007 the original author or authors.
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.springframework.integration.message.MessagingException;
import org.springframework.xml.transform.StringResult;

/**
 * Converts the passed {@link Result} to an instance of {@link String}
 * 
 * Supports {@link StringResult} and {@link DOMResult}
 * 
 * @author Jonas Partner
 * 
 */
public class ResultToStringTransformer implements ResultTransformer {

	private DocumentBuilderFactory docBuilderFactory;

	private TransformerFactory transformerFactory;

	public ResultToStringTransformer() {
		this.docBuilderFactory = DocumentBuilderFactory.newInstance();
		this.transformerFactory = TransformerFactory.newInstance();
	}

	protected Transformer getNewTransformer()
			throws TransformerConfigurationException {
		synchronized (transformerFactory) {
			return transformerFactory.newTransformer();
		}
	}

	public Object transformResult(Result res) {
		String returnString = null;
		if (res instanceof StringResult) {
			returnString = ((StringResult) res).toString();
		} else if (res instanceof DOMResult) {
			try {
				StringResult strRes = new StringResult();
				getNewTransformer().transform(
						new DOMSource(((DOMResult) res).getNode()), strRes);
				returnString = strRes.toString();
			} catch (TransformerException transE) {
				throw new MessagingException(
						"Transformation from DOMSOurce failed", transE);
			}
		}

		if (returnString == null) {
			throw new MessagingException("Could not convert Result type "
					+ res.getClass().getName() + " to string");
		}

		return returnString;
	}

	protected DocumentBuilder getNewDocumentBuilder()
			throws ParserConfigurationException {
		synchronized (docBuilderFactory) {
			return docBuilderFactory.newDocumentBuilder();
		}

	}

}
