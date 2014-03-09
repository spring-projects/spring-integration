/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Map;

import org.w3c.dom.Node;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.transformer.HeaderEnricher;
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
 * Transformer implementation that evaluates XPath expressions against the
 * message payload and inserts the result of the evaluation into a message
 * header. The header names will match the keys in the map of expressions.
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
public class XPathHeaderEnricher extends HeaderEnricher {

	/**
	 * Create an instance of XPathHeaderEnricher using a map with header names as keys
	 * and XPathExpressionValueHolders to evaluate the values.
	 *
	 * @param expressionMap The expression map.
	 */
	public XPathHeaderEnricher(Map<String, XPathExpressionEvaluatingHeaderValueMessageProcessor> expressionMap) {
		super(expressionMap);
	}


	public static class XPathExpressionEvaluatingHeaderValueMessageProcessor implements HeaderValueMessageProcessor<Object>,
			BeanFactoryAware {

		private static final XmlPayloadConverter converter = new DefaultXmlPayloadConverter();

		private final BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();

		private final XPathExpression expression;

		private volatile XPathEvaluationType evaluationType = XPathEvaluationType.STRING_RESULT;

		private volatile TypeDescriptor headerTypeDescriptor;

		private volatile Boolean overwrite = null;

		public XPathExpressionEvaluatingHeaderValueMessageProcessor(String expression) {
			Assert.hasText(expression, "expression must have text");
			this.expression = XPathExpressionFactory.createXPathExpression(expression);
		}

		public XPathExpressionEvaluatingHeaderValueMessageProcessor(XPathExpression expression) {
			Assert.notNull(expression, "expression must not be null");
			this.expression = expression;
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
			ConversionService conversionService = IntegrationContextUtils.getConversionService(beanFactory);
			if (conversionService != null) {
				this.typeConverter.setConversionService(conversionService);
			}
		}

		@Override
		public Object processMessage(Message<?> message) {
			Node node = converter.convertToNode(message.getPayload());
			Object result = this.evaluationType.evaluateXPath(this.expression, node);
			if (result instanceof String && ((String) result).length() == 0) {
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

}
