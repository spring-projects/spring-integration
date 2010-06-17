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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Will parse 'channel-interceptor-chain' element
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class GlobalChannelInterceptorParser extends AbstractBeanDefinitionParser {
	private static final String CONFIG_PACKAGE = IntegrationNamespaceUtils.BASE_PACKAGE + ".channel.interceptor.";
	private final ManagedList<RuntimeBeanReference> globalInterceptors = new ManagedList<RuntimeBeanReference>();
	private final static String CHANNELL_NAME_PATTERN_ATTR = "pattern";
	private final static String REF_ATTR = "ref";
	private final static String ORDER_ATTR = "order";
	private final String GLOBAL_POST_PROCESSOR_CLASS = "GlobalChannelInterceptorBeanPostProcessor";
	
	private boolean postProcessorCreated;
	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.AbstractBeanDefinitionParser#parseInternal(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)
	 */
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		this.createAndRegisterGlobalPostProcessorIfNeeded(parserContext);
		
		
		
		int order = this.getOrderAttribute(element);
		String channelPattern = element.getAttribute(CHANNELL_NAME_PATTERN_ATTR);
		
		BeanDefinitionBuilder globalChannelInterceptorBuilder =
			BeanDefinitionBuilder.genericBeanDefinition(CONFIG_PACKAGE + "GlobalChannelInterceptorWrapper");
		BeanComponentDefinition interceptorBeanDefinition = IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		if (interceptorBeanDefinition != null){
			globalChannelInterceptorBuilder.addConstructorArgValue(interceptorBeanDefinition);
		} else {
			String beanName = element.getAttribute(REF_ATTR);
			globalChannelInterceptorBuilder.addConstructorArgValue(new RuntimeBeanReference(beanName));
		}
		
		globalChannelInterceptorBuilder.addPropertyValue("order", order);
		String[] patterns = null;
		if (StringUtils.hasText(channelPattern)){
			patterns =  StringUtils.commaDelimitedListToStringArray(channelPattern);
		} else {
			patterns = new String[]{"*"};
		}
		globalChannelInterceptorBuilder.addPropertyValue("patterns", patterns);

		String beanName = 
			BeanDefinitionReaderUtils.generateBeanName(globalChannelInterceptorBuilder.getBeanDefinition(), parserContext.getRegistry());
		parserContext.registerBeanComponent(new BeanComponentDefinition(globalChannelInterceptorBuilder.getBeanDefinition(), beanName));
		globalInterceptors.add(new RuntimeBeanReference(beanName));
//		
//		
//		*
//		
//		
//		
//		
//		
//		List<Element> interceptorElements = DomUtils.getChildElementsByTagName(element, new String[]{REF_ATTR, BEAN_ATTR});
//		
//		BeanDefinitionBuilder globalChannelInterceptorBuilder =
//			BeanDefinitionBuilder.genericBeanDefinition(CONFIG_PACKAGE + INTERCEPTOR_CHAIN_CLASS);
//		String[] channelPatterns = element.getAttribute(CHANNELL_NAME_PATTERN_ATTR).split(",");
//		int order = this.getOrderAttribute(element);
//		
//		ManagedList<RuntimeBeanReference> adviceChain = new ManagedList<RuntimeBeanReference>();
//		for (Element interceptorElement : interceptorElements) {
//			if (interceptorElement.getNodeName().equals(BEAN_ATTR)){
//				BeanDefinitionParserDelegate delegate = parserContext.getDelegate();
//				BeanDefinitionHolder holder = delegate.parseBeanDefinitionElement(interceptorElement);
//				// needed for p: namespace
//				holder = delegate.decorateBeanDefinitionIfRequired(interceptorElement, holder);
//				parserContext.registerBeanComponent(new BeanComponentDefinition(holder));
//				adviceChain.add(new RuntimeBeanReference(holder.getBeanName()));
//			} else if (interceptorElement.getNodeName().equals(REF_ATTR)) {
//				String ref = interceptorElement.getAttribute(BEAN_ATTR);
//				adviceChain.add(new RuntimeBeanReference(ref));
//			}
//		}
//		globalChannelInterceptorBuilder.addConstructorArgValue(adviceChain);
//		globalChannelInterceptorBuilder.addConstructorArgValue(channelPatterns);
//		globalChannelInterceptorBuilder.addConstructorArgValue(order);
//		AbstractBeanDefinition interceptorChainDef = globalChannelInterceptorBuilder.getBeanDefinition();
//		String interceptorChainName = 
//			BeanDefinitionReaderUtils.generateBeanName(interceptorChainDef, parserContext.getRegistry());
//		parserContext.registerBeanComponent(new BeanComponentDefinition(interceptorChainDef, interceptorChainName));	
//		globalInterceptorChains.add(new RuntimeBeanReference(interceptorChainName));
		return null;
	}
	/*
	 * 
	 */
	private int getOrderAttribute(Element element){
		String sOrder = element.getAttribute(ORDER_ATTR);
		if (StringUtils.hasText(sOrder)){
			return Integer.parseInt(sOrder);
		}
		return 0;
	}
	/*
	 * 
	 */
	private void createAndRegisterGlobalPostProcessorIfNeeded(ParserContext parserContext){
		if (!postProcessorCreated){
			BeanDefinitionBuilder postProcessorBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(CONFIG_PACKAGE + GLOBAL_POST_PROCESSOR_CLASS);
			BeanDefinition beanDef = postProcessorBuilder.getBeanDefinition();
			postProcessorBuilder.addConstructorArgValue(globalInterceptors);
			String beanName = 
				BeanDefinitionReaderUtils.generateBeanName(beanDef, parserContext.getRegistry());
			parserContext.registerBeanComponent(new BeanComponentDefinition(beanDef, beanName));
			postProcessorCreated = true;
		}
	}
}
