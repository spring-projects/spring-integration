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

package org.springframework.integration.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.endpoint.interceptor.TransactionInterceptor;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.util.StringUtils;

/**
 * Parser for the <em>transaction-interceptor</em> element.
 * 
 * @author Mark Fisher
 */
public class TransactionInterceptorParser implements BeanDefinitionRegisteringParser {

	@SuppressWarnings("unchecked")
	public String parse(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(TransactionInterceptor.class);
		String txManagerRef = element.getAttribute("transaction-manager");
		if (!StringUtils.hasText(txManagerRef)) {
			txManagerRef = "transactionManager";
		}
		builder.addConstructorArgReference(txManagerRef);
		String propagation = element.getAttribute("propagation");
		String isolation = element.getAttribute("isolation");
		String timeout = element.getAttribute("timeout");
		String readOnly = element.getAttribute("read-only");
		if (StringUtils.hasText(propagation)) {
			builder.addPropertyValue("propagationBehaviorName", RuleBasedTransactionAttribute.PREFIX_PROPAGATION + propagation);
		}
		if (StringUtils.hasText(isolation)) {
			builder.addPropertyValue("isolationLevelName", RuleBasedTransactionAttribute.PREFIX_ISOLATION + isolation);
		}
		if (StringUtils.hasText(timeout)) {
			try {
				builder.addPropertyValue("timeout", Integer.parseInt(timeout));
			}
			catch (NumberFormatException ex) {
				parserContext.getReaderContext().error("Timeout must be an integer value: [" + timeout + "]", element);
			}
		}
		if (StringUtils.hasText(readOnly)) {
			builder.addPropertyValue("readOnly", Boolean.valueOf(readOnly));
		}
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
