/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.jpa.config.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * The common method for generating the BeanDefinition for the common MessageHandler
 * is implemented in this class
 * 
 * @author Amol Nayak
 * @since 2.2
 * 
 */
public class JpaParserUtils {

	/**
	 * @param element
	 * @param parserContext
	 * @return
	 */
	public static BeanDefinitionBuilder getMessageHandlerBuilder(Element element,
			ParserContext parserContext) {
		String jpaOperations = getJpaOperationsRefName(element,parserContext);
		BeanDefinitionBuilder  messageHandlerFactoryBuilder = 
				BeanDefinitionBuilder.genericBeanDefinition("JpaMessageHandlerFactory.class"); //FIXME
		messageHandlerFactoryBuilder.addPropertyReference("jpaOperations", jpaOperations);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(messageHandlerFactoryBuilder, element, "jpa-ql");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(messageHandlerFactoryBuilder, element, "parameter-source-factory","requestParameterSourceFactory");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(messageHandlerFactoryBuilder, element, "parameter-source-factory","requestParameterSourceFactory");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(messageHandlerFactoryBuilder, element, "reply-parameter-source-factory");
		Element txElement = DomUtils.getChildElementByTagName(element, "transactional");
		if(txElement != null) {
			BeanDefinition txAdviceDefinition = getTransactionInterceptor(txElement);
			messageHandlerFactoryBuilder.addPropertyValue("transactionAdvice", txAdviceDefinition);
		}
		return messageHandlerFactoryBuilder;
	}
	
	/**
	 * Create a new BeanDefinition for the transaction intercepter based on the transactional sub element
	 * @param txElement
	 * @return
	 */
	private static BeanDefinition getTransactionInterceptor(Element txElement) {
		BeanDefinitionBuilder interceptorBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition(TransactionInterceptor.class);
		interceptorBuilder.addPropertyReference("transactionManager", txElement.getAttribute("transaction-manager"));
		BeanDefinitionBuilder txAttribute = BeanDefinitionBuilder.genericBeanDefinition(DefaultTransactionAttribute.class);
		txAttribute.addPropertyValue("isolationLevelName", "ISOLATION_" + txElement.getAttribute("isolation"));
		txAttribute.addPropertyValue("propagationBehaviorName", "PROPAGATION_" + txElement.getAttribute("propagation"));
		txAttribute.addPropertyValue("timeout", txElement.getAttribute("timeout"));
		txAttribute.addPropertyValue("readOnly", txElement.getAttribute("read-only"));
		
		BeanDefinitionBuilder txnAttributeSource = 
			BeanDefinitionBuilder.genericBeanDefinition(MatchAlwaysTransactionAttributeSource.class);
		txnAttributeSource.addPropertyValue("transactionAttribute", txAttribute.getBeanDefinition());
		interceptorBuilder.addPropertyValue("transactionAttributeSource", txnAttributeSource.getBeanDefinition());
		
		return interceptorBuilder.getBeanDefinition();
	}
	
	
	private static String getJpaOperationsRefName(Element element,ParserContext parserContext) {
//		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JpaOperationsImpl.class);
//		
//		BeanDefinitionBuilder emBuilder = JpaParserUtils.entityManagerBuilder(element.getAttribute("entity-manager-factory"));
//		
//		builder.addConstructorArgValue(emBuilder.getBeanDefinition());
//		
//		return BeanDefinitionReaderUtils
//					.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
		return null;
	}

}
