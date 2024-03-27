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

import org.w3c.dom.Node;

import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.integration.xml.xpath.XPathEvaluationType;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.xml.xpath.NodeMapper;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/**
 * Transformer implementation that evaluates an XPath expression against the inbound
 * Message payload and returns a Message whose payload is the result of that evaluation.
 * Prior to evaluation, the payload may be converted by the configured {@link XmlPayloadConverter}
 * instance. The default converter type is {@link DefaultXmlPayloadConverter}.
 * <p>
 * The evaluation result type will depend on either the enumeration value provided to
 * {@link #setEvaluationType(XPathEvaluationType)} or the presence of a {@link NodeMapper},
 * which takes precedence. If no {@link NodeMapper} or evaluation type is configured explicitly,
 * the default evaluation type is {@link XPathEvaluationType#STRING_RESULT}.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class XPathTransformer extends AbstractTransformer {

	private final XPathExpression xpathExpression;

	private volatile XmlPayloadConverter converter = new DefaultXmlPayloadConverter();

	private volatile XPathEvaluationType evaluationType = XPathEvaluationType.STRING_RESULT;

	private volatile NodeMapper<?> nodeMapper;

	/**
	 * Create an {@link XPathTransformer} that will create an XPath expression from the given String
	 * to be evaluated against converted inbound Message payloads.
	 *
	 * @param expression The expression.
	 */
	public XPathTransformer(String expression) {
		this.xpathExpression = XPathExpressionFactory.createXPathExpression(expression);
	}

	/**
	 * Create an {@link XPathTransformer} that will evaluate the given {@link XPathExpression}
	 * against converted inbound Message payloads.
	 *
	 * @param expression The expression.
	 */
	public XPathTransformer(XPathExpression expression) {
		Assert.notNull(expression, "expression must not be null");
		this.xpathExpression = expression;
	}

	/**
	 * Specify the expected {@link XPathEvaluationType}. The default is {@link XPathEvaluationType#STRING_RESULT}.
	 *
	 * @param evaluationType The evaluation type.
	 */
	public void setEvaluationType(XPathEvaluationType evaluationType) {
		Assert.notNull(evaluationType, "evaluationType must not be null.");
		this.evaluationType = evaluationType;
	}

	/**
	 * Set a {@link NodeMapper} to use for generating the result object. By default the NodeMapper is null,
	 * but if explicitly set, type determination is the responsibility of the NodeMapper, taking precedence
	 * over any configured evaluationType.
	 *
	 * @param nodeMapper The node mapper.
	 */
	public void setNodeMapper(NodeMapper<?> nodeMapper) {
		this.nodeMapper = nodeMapper;
	}

	/**
	 * Specify the {@link XmlPayloadConverter} to use when converting a Message payload prior to XPath evaluation.
	 *
	 * @param converter The payload converter.
	 */
	public void setConverter(XmlPayloadConverter converter) {
		Assert.notNull(converter, "converter must not be null");
		this.converter = converter;
	}

	@Override
	public String getComponentType() {
		return "xml:xpath-transformer";
	}

	@Override
	protected Object doTransform(Message<?> message) {
		Node node = this.converter.convertToNode(message.getPayload());
		Object result = null;
		if (this.nodeMapper != null) {
			result = this.xpathExpression.evaluateAsObject(node, this.nodeMapper);
		}
		else {
			result = this.evaluationType.evaluateXPath(this.xpathExpression, node);
		}
		return result;
	}

}
