/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.xml.transformer.support;

import org.w3c.dom.Node;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.integration.util.BeanFactoryTypeConverter;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.integration.xml.xpath.XPathEvaluationType;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/**
 * The xPath-specific {@link HeaderValueMessageProcessor}
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Ngoc Nhan
 *
 * @since 2.0
 */
public class XPathExpressionEvaluatingHeaderValueMessageProcessor implements HeaderValueMessageProcessor<Object>,
		BeanFactoryAware {

	private final BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();

	private final XPathExpression expression;

	private final XmlPayloadConverter converter;

	private XPathEvaluationType evaluationType = XPathEvaluationType.STRING_RESULT;

	private TypeDescriptor headerTypeDescriptor;

	private Boolean overwrite;

	public XPathExpressionEvaluatingHeaderValueMessageProcessor(String expression) {
		this(expression, new DefaultXmlPayloadConverter());
	}

	/**
	 * Construct an instance based on the provided xpath expression and {@link XmlPayloadConverter}.
	 * @param expression the xpath expression to evaluate.
	 * @param converter the {@link XmlPayloadConverter} to use for document conversion.
	 * @since 4.3.19
	 */
	public XPathExpressionEvaluatingHeaderValueMessageProcessor(String expression, XmlPayloadConverter converter) {
		this(XPathExpressionFactory.createXPathExpression(expression), converter);
	}

	public XPathExpressionEvaluatingHeaderValueMessageProcessor(XPathExpression expression) {
		this(expression, new DefaultXmlPayloadConverter());
	}

	/**
	 * Construct an instance based on the provided xpath expression and {@link XmlPayloadConverter}.
	 * @param expression the xpath expression to evaluate.
	 * @param converter the {@link XmlPayloadConverter} to use for document conversion.
	 * @since 4.3.19
	 */
	public XPathExpressionEvaluatingHeaderValueMessageProcessor(XPathExpression expression,
			XmlPayloadConverter converter) {

		Assert.notNull(expression, "'expression' must not be null.");
		Assert.notNull(converter, "'converter' must not be null.");
		this.expression = expression;
		this.converter = converter;
	}

	public void setEvaluationType(XPathEvaluationType evaluationType) {
		this.evaluationType = evaluationType;
	}

	public void setHeaderType(Class<?> headerType) {
		if (headerType != null) {
			this.headerTypeDescriptor = TypeDescriptor.valueOf(headerType);
		}
	}

	public void setOverwrite(Boolean overwrite) {
		this.overwrite = overwrite;
	}

	@Override
	public Boolean isOverwrite() {
		return this.overwrite;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		ConversionService conversionService = IntegrationUtils.getConversionService(beanFactory);
		if (conversionService != null) {
			this.typeConverter.setConversionService(conversionService);
		}
	}

	@Override
	public Object processMessage(Message<?> message) {
		Node node = this.converter.convertToNode(message.getPayload());
		Object result = this.evaluationType.evaluateXPath(this.expression, node);
		if (result instanceof String string && string.isEmpty()) {
			result = null;
		}
		if (result != null && this.headerTypeDescriptor != null) {
			return this.typeConverter.convertValue(result, TypeDescriptor.forObject(result), this.headerTypeDescriptor);
		}
		else {
			return result;
		}
	}

}
