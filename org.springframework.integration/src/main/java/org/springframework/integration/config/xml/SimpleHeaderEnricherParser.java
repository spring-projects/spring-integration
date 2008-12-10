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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.util.ObjectUtils;

/**
 * @author Mark Fisher
 */
public class SimpleHeaderEnricherParser extends AbstractTransformerParser {

	private static String[] ineligibleHeaderNames = new String[] {
		"id", "input-channel", "output-channel", "overwrite"
	};


	private final String prefix;

	private List<String> referenceAttributes;


	public SimpleHeaderEnricherParser() {
		this(null, null);
	}

	public SimpleHeaderEnricherParser(String prefix) {
		this(prefix, null);
	}

	public SimpleHeaderEnricherParser(String prefix, String[] referenceAttributes) {
		this.prefix = prefix;
		this.referenceAttributes = (referenceAttributes != null)
				? Arrays.asList(referenceAttributes) : Collections.<String>emptyList();
	}


	@Override
	protected String getTransformerClassName() {
		return IntegrationNamespaceUtils.BASE_PACKAGE + ".transformer.HeaderEnricher";
	}

	protected boolean isEligibleHeaderName(String headerName) {
		return !(ObjectUtils.containsElement(ineligibleHeaderNames, headerName));
	}

	protected boolean shouldOverwrite(Element element) {
		return "true".equals(element.getAttribute("overwrite").toLowerCase());
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		ManagedMap headers = new ManagedMap();
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node node = attributes.item(i);
			String name = node.getNodeName();
			if (this.isEligibleHeaderName(name)) {
				name = Conventions.attributeNameToPropertyName(name);
				Object value = (this.referenceAttributes.contains(name))
						? new RuntimeBeanReference(node.getNodeValue())
						: node.getNodeValue();
				if (this.prefix != null) {
					name = this.prefix + name;
				}
				headers.put(name, value);
			}
		}
		this.postProcessHeaders(element, headers);
		builder.addConstructorArgValue(headers);
		builder.addPropertyValue("overwrite", this.shouldOverwrite(element));
	}

	/**
	 * Subclasses may implement this method to provide additional headers.
	 */
	protected void postProcessHeaders(Element element, ManagedMap headers) {
	}

}
