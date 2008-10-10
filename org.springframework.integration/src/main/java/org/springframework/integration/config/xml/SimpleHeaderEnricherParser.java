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

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.Transformer;
import org.springframework.util.ObjectUtils;

/**
 * @author Mark Fisher
 */
public class SimpleHeaderEnricherParser extends AbstractTransformerParser {

	private static String[] ineligibleHeaderNames = new String[] {
		"id", "input-channel", "output-channel", "overwrite"
	};


	private final String prefix;


	public SimpleHeaderEnricherParser() {
		this(null);
	}

	public SimpleHeaderEnricherParser(String prefix) {
		this.prefix = prefix;
	}


	@Override
	protected Class<? extends Transformer> getTransformerClass() {
		return HeaderEnricher.class;
	}

	protected boolean isEligibleHeaderName(String headerName) {
		return !(ObjectUtils.containsElement(ineligibleHeaderNames, headerName));
	}

	protected boolean shouldOverwrite(Element element) {
		return "true".equals(element.getAttribute("overwrite").toLowerCase());
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		Map<String, Object> headersToAdd = new HashMap<String, Object>();
		NamedNodeMap nodeMap = element.getAttributes();
		for (int i = 0; i < nodeMap.getLength(); i++) {
			Node node = nodeMap.item(i);
			String name = node.getNodeName();
			if (this.isEligibleHeaderName(name)) {
				headersToAdd.put(name, node.getNodeValue());				
			}
		}
		builder.addConstructorArgValue(headersToAdd);
		builder.addPropertyValue("overwrite", this.shouldOverwrite(element));
		if (this.prefix != null) {
			builder.addPropertyValue("prefix", this.prefix);
		}
	}

}
