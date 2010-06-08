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

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;

import org.springframework.integration.core.Message;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.integration.xml.xpath.XPathEvaluationType;
import org.springframework.util.Assert;
import org.springframework.xml.xpath.NodeMapper;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/**
 * Transformer implementation that evaluates an XPath expression against the inbound
 * Message payload and returns a Message whose payload is the result of that evaluation.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class XPathTransformer extends AbstractTransformer {

	private final XPathExpression xpathExpression;

	private volatile XmlPayloadConverter converter = new DefaultXmlPayloadConverter();

	private volatile XPathEvaluationType evaluationType = XPathEvaluationType.STRING_RESULT;

	private volatile NodeMapper nodeMapper;


	public XPathTransformer(String expression) {
		this.xpathExpression = XPathExpressionFactory.createXPathExpression(expression);		
	}

	public XPathTransformer(String expression, Map<String, String> namespaces) {
		this.xpathExpression = XPathExpressionFactory.createXPathExpression(expression, namespaces);
	}

	public XPathTransformer(String expression, String prefix, String namespace) {
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put(prefix, namespace);
		this.xpathExpression = XPathExpressionFactory.createXPathExpression(expression, namespaces);
	}


	/**
	 * Specify the expected {@link XPathEvaluationType}. The default is {@link XPathEvaluationType#STRING_RESULT}.
	 */
	public void setEvaluationType(XPathEvaluationType evaluationType) {
		this.evaluationType = evaluationType;
	}

	/**
	 * Set a {@link NodeMapper} to use for generating the result object.
	 * This will also set the evaluationType to <code>null</code> since
	 * the actual type determination is a responsibility of the NodeMapper.
	 */
	public void setNodeMapper(NodeMapper nodeMapper) {
		this.evaluationType = null;
		this.nodeMapper = nodeMapper;
	}

	/**
	 * Specify the {@link XmlPayloadConverter} to use when converting a Message payload prior to XPath evaluation.
	 */
	public void setConverter(XmlPayloadConverter converter) {
		Assert.notNull(converter, "converter must not be null");
		this.converter = converter;
	}

	@Override
	protected Object doTransform(Message<?> message) throws Exception {
		Node node = this.converter.convertToNode(message.getPayload());
		Object result = null;
		if (evaluationType == null) {
			Assert.notNull(this.nodeMapper, "NodeMapper required if evaluationType is null.");
			result = this.xpathExpression.evaluateAsObject(node, this.nodeMapper);
		}
		else {
			result = this.evaluationType.evaluateXPath(this.xpathExpression, node);
		}
		return result;
	}

}
