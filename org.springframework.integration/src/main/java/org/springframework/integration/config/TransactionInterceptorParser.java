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

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
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
		builder.addPropertyReference("transactionManager", txManagerRef);
		RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
		String propagation = element.getAttribute("propagation");
		String isolation = element.getAttribute("isolation");
		String timeout = element.getAttribute("timeout");
		String readOnly = element.getAttribute("read-only");
		if (StringUtils.hasText(propagation)) {
			attribute.setPropagationBehaviorName(RuleBasedTransactionAttribute.PREFIX_PROPAGATION + propagation);
		}
		if (StringUtils.hasText(isolation)) {
			attribute.setIsolationLevelName(RuleBasedTransactionAttribute.PREFIX_ISOLATION + isolation);
		}
		if (StringUtils.hasText(timeout)) {
			try {
				attribute.setTimeout(Integer.parseInt(timeout));
			}
			catch (NumberFormatException ex) {
				parserContext.getReaderContext().error("Timeout must be an integer value: [" + timeout + "]", element);
			}
		}
		if (StringUtils.hasText(readOnly)) {
			attribute.setReadOnly(Boolean.valueOf(readOnly).booleanValue());
		}
		List rollbackRules = new LinkedList();
		if (element.hasAttribute("rollback-for")) {
			String rollbackForValue = element.getAttribute("rollback-for");
			String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(rollbackForValue);
			for (int i = 0; i < exceptionTypeNames.length; i++) {
				rollbackRules.add(new RollbackRuleAttribute(StringUtils.trimWhitespace(exceptionTypeNames[i])));
			}
		}
		if (element.hasAttribute("no-rollback-for")) {
			String noRollbackForValue = element.getAttribute("no-rollback-for");
			String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(noRollbackForValue);
			for (int i = 0; i < exceptionTypeNames.length; i++) {
				rollbackRules.add(new NoRollbackRuleAttribute(StringUtils.trimWhitespace(exceptionTypeNames[i])));
			}
		}
		attribute.setRollbackRules(rollbackRules);
		RootBeanDefinition attributeSourceDefinition = new RootBeanDefinition(MatchAlwaysTransactionAttributeSource.class);
		attributeSourceDefinition.setSource(parserContext.extractSource(element));
		attributeSourceDefinition.getPropertyValues().addPropertyValue("transactionAttribute", attribute);
		String attributeSourceBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
		attributeSourceDefinition, parserContext.getRegistry());
		builder.addPropertyReference("transactionAttributeSource", attributeSourceBeanName);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
