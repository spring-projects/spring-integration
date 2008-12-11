/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;header-enricher&gt; element within the core integration
 * namespace. This is used for setting the <em>standard</em>, out-of-the-box
 * configurable {@link MessageHeaders}, such as 'reply-channel', 'priority',
 * and 'correlation-id'. It will also accept custom header values (or bean
 * references) if provided as 'header' sub-elements.
 * 
 * @author Mark Fisher
 */
public class StandardHeaderEnricherParser extends SimpleHeaderEnricherParser {

	private static final String[] REFERENCE_ATTRIBUTES = new String[] {
		"reply-channel", "error-channel"
	};


	public StandardHeaderEnricherParser() {
		super(MessageHeaders.PREFIX, REFERENCE_ATTRIBUTES);
	}


	@Override
	@SuppressWarnings("unchecked")
	protected void postProcessHeaders(Element element, ManagedMap headers, ParserContext parserContext) {
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && node.getLocalName().equals("header")) {
				Element headerElement = (Element) node;
				String name = headerElement.getAttribute("name");
				String value = headerElement.getAttribute("value");
				String ref = headerElement.getAttribute("ref");
				boolean isValue = StringUtils.hasText(value);
				boolean isRef = StringUtils.hasText(ref);
				if (!(isValue ^ isRef)) {
					parserContext.getReaderContext().error(
							"Exactly one of the 'value' or 'ref' attributes is required.", element);
				}
				if (isValue) {
					headers.put(name, value);
				}
				else {
					headers.put(name, new RuntimeBeanReference(ref));
				}
			}
		}
	}

}
