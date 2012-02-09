/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Ordered;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base class parser for elements that create Message Endpoints.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public abstract class AbstractConsumerEndpointParser extends AbstractBeanDefinitionParser {

	protected static final String REF_ATTRIBUTE = "ref";

	protected static final String METHOD_ATTRIBUTE = "method";

	protected static final String EXPRESSION_ATTRIBUTE = "expression";
	
	protected static final String CHANNEL_CREATOR_BEAN_NAME = "$_inputChannelCreator";
	
	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	/**
	 * Parse the MessageHandler.
	 */
	protected abstract BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext);

	protected String getInputChannelAttributeName() {
		return "input-channel";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected final AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder handlerBuilder = this.parseHandler(element, parserContext);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(handlerBuilder, element, "output-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(handlerBuilder, element, "order");
		AbstractBeanDefinition handlerBeanDefinition = handlerBuilder.getBeanDefinition();
		String inputChannelAttributeName = this.getInputChannelAttributeName();
		if (!element.hasAttribute(inputChannelAttributeName)) {
			if (!parserContext.isNested()) {
				String elementDescription = IntegrationNamespaceUtils.createElementDescription(element);
				parserContext.getReaderContext().error("The '" + inputChannelAttributeName
						+ "' attribute is required for the top-level endpoint element "
						+ elementDescription + ".", element);
			}
			return handlerBeanDefinition;
		}

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ConsumerEndpointFactoryBean.class);

		String handlerBeanName = BeanDefinitionReaderUtils.generateBeanName(handlerBeanDefinition, parserContext.getRegistry());
		parserContext.registerBeanComponent(new BeanComponentDefinition(handlerBeanDefinition, handlerBeanName));
		
		builder.addPropertyReference("handler", handlerBeanName);
		String inputChannelName = element.getAttribute(inputChannelAttributeName);
		
		if (parserContext.getRegistry().containsBeanDefinition(CHANNEL_CREATOR_BEAN_NAME)){
			BeanDefinition definition = parserContext.getRegistry().getBeanDefinition(CHANNEL_CREATOR_BEAN_NAME);
			Set<String> channelNames = (Set<String>) definition.getConstructorArgumentValues().getArgumentValue(0, Set.class).getValue();
			channelNames.add(inputChannelName);
		}
		else {
			BeanDefinitionBuilder channelDef = BeanDefinitionBuilder.genericBeanDefinition(ChannelCreatingPostProcessor.class);
			Set<String> channelNames = new HashSet<String>();
			channelNames.add(inputChannelName);
			channelDef.addConstructorArgValue(channelNames);
			BeanDefinitionHolder channelCreatorHolder = new BeanDefinitionHolder(channelDef.getBeanDefinition(), CHANNEL_CREATOR_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(channelCreatorHolder, parserContext.getRegistry());
		}
			
		builder.addPropertyValue("inputChannelName", inputChannelName);
		List<Element> pollerElementList = DomUtils.getChildElementsByTagName(element, "poller");
		if (!CollectionUtils.isEmpty(pollerElementList)) {
			if (pollerElementList.size() != 1) {
				parserContext.getReaderContext().error(
						"at most one poller element may be configured for an endpoint", element);
			}
			IntegrationNamespaceUtils.configurePollerMetadata(pollerElementList.get(0), builder, parserContext);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
		String beanName = this.resolveId(element, beanDefinition, parserContext);
		parserContext.registerBeanComponent(new BeanComponentDefinition(beanDefinition, beanName));
		return null;
	}

	private static class ChannelCreatingPostProcessor implements BeanFactoryPostProcessor, Ordered {
		
		private final Set<String> channels;
		
		@SuppressWarnings("unused")
		public ChannelCreatingPostProcessor(Set<String> channels){
			this.channels = channels;
		}
	
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

			BeanExpressionResolver beanExpressionResolver = beanFactory.getBeanExpressionResolver();
			
			for (String channelName : this.channels) {
				if (beanExpressionResolver != null) {
					channelName = (String) beanExpressionResolver.evaluate(channelName, new BeanExpressionContext(beanFactory, null));
				}
				if (!beanFactory.containsBean(channelName) && this.shouldAutoCreateChannel(channelName)){		
					BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
					RootBeanDefinition messageChannel = new RootBeanDefinition();
					messageChannel.setBeanClass(DirectChannel.class);
					BeanDefinitionHolder messageChannelHolder = new BeanDefinitionHolder(messageChannel, channelName);
					BeanDefinitionReaderUtils.registerBeanDefinition(messageChannelHolder, registry);
				}
			}		
		}
		
		private boolean shouldAutoCreateChannel(String channelName) {
			return !IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME.equals(channelName)
					&& !IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME.equals(channelName);
		}
		/**
		 * Within the context of Spring Integration this Factory Post Processor must be invoked *first*. 
		 * It must also be invoked *after* PropertyPlaceholderConfigurer etc., becouse channel names in the 
		 * 'channels' Set might contain property placeholders. Given the fact that the order of BeanFactoryPostProcessor 
		 * invocation in AbstractApplicationContext.invokeBeanFactoryPostProcessors(..) is PriorityOrdered -> Ordered -> Regular FPP and 
		 * that PropertyPlaceholderConfigurer implements PriorityOrdered this post processor 
		 * implements Ordered with HIGHEST_PRECEDENCE.
		 */
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}
	}
}
