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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
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

	@Override
	protected final AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder handlerBuilder = this.parseHandler(element, parserContext);
		
		String outChannelValue = element.getAttribute("output-channel");
		if (StringUtils.hasText(outChannelValue)){
			BeanDefinitionBuilder outDef = BeanDefinitionBuilder.genericBeanDefinition(OutputChannelFactoryBean.class);
			outDef.addConstructorArgValue(outChannelValue);
			handlerBuilder.addPropertyValue("outputChannel", outDef.getBeanDefinition());
		}
					
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
		
		BeanDefinitionBuilder strDef = BeanDefinitionBuilder.genericBeanDefinition(InputChannelNameFactoryBean.class);
		strDef.addConstructorArgValue(inputChannelName);
		BeanDefinitionReaderUtils.registerWithGeneratedName(strDef.getBeanDefinition(), parserContext.getRegistry());
		builder.addPropertyValue("inputChannelName", strDef.getBeanDefinition());
			
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
	
	private static class InputChannelNameFactoryBean implements FactoryBean<String>, BeanFactoryAware{
		
		private final Log logger = LogFactory.getLog(this.getClass());
		
		private final String channelName;

		private volatile BeanFactory beanFactory;
		
		@SuppressWarnings("unused")
		public InputChannelNameFactoryBean(String channelName){
			this.channelName = channelName;
		}

		public String getObject() throws Exception {
			if (!this.beanFactory.containsBean(this.channelName) && this.shouldAutoCreateChannel(channelName)){
				logger.info("Channel '" + this.channelName + "' is not explicitly defined. Will auto-create as DirectChannel");
				RootBeanDefinition messageChannel = new RootBeanDefinition();
				messageChannel.setBeanClass(DirectChannel.class);
				BeanDefinitionHolder messageChannelHolder = new BeanDefinitionHolder(messageChannel, channelName);
				BeanDefinitionReaderUtils.registerBeanDefinition(messageChannelHolder, (BeanDefinitionRegistry)beanFactory);
			}
			return this.channelName;
		}
		
		public Class<?> getObjectType() {
			return String.class;
		}

		public boolean isSingleton() {
			return true;
		}

		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}
		
		private boolean shouldAutoCreateChannel(String channelName) {
			return !IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME.equals(channelName)
					&& !IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME.equals(channelName);
		}
	}
	
	private static class OutputChannelFactoryBean implements FactoryBean<MessageChannel>, BeanFactoryAware{
		private final String channelName;
		private volatile ConfigurableListableBeanFactory beanFactory;
		
		@SuppressWarnings("unused")
		public OutputChannelFactoryBean(String channelName){
			this.channelName = channelName;
		}

		public MessageChannel getObject() throws Exception {
			// the following line will force creation of all InputChannelFactoryBean, thus creating channels
			this.beanFactory.getBeansOfType(InputChannelNameFactoryBean.class);
			
			return this.beanFactory.getBean(this.channelName, MessageChannel.class);
		}

		public Class<?> getObjectType() {
			return MessageChannel.class;
		}

		public boolean isSingleton() {
			return true;
		}

		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;		
		}
	}
}
