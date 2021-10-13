/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.IntegrationConfigUtils;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;chain&gt; element.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Gary Russell
 */
public class ChainParser extends AbstractConsumerEndpointParser {

	/**
	 * {@link BeanDefinition} attribute used to pass down the current bean id for nested chains, allowing full
	 * qualification of 'named' handlers within nested chains.
	 *
	 */
	private static final String SI_CHAIN_NESTED_ID_ATTRIBUTE = "SI.ChainParser.NestedId.Prefix";

	private final Log logger = LogFactory.getLog(this.getClass());

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MessageHandlerChain.class);

		if (!StringUtils.hasText(element.getAttribute(ID_ATTRIBUTE))) {
			this.logger.info("It is useful to provide an explicit 'id' attribute on 'chain' elements " +
					"to simplify the identification of child elements in logs etc.");
		}

		String chainHandlerId = this.resolveId(element, builder.getRawBeanDefinition(), parserContext);
		List<BeanMetadataElement> handlerList = new ManagedList<BeanMetadataElement>();
		Set<String> handlerBeanNameSet = new HashSet<String>();
		NodeList children = element.getChildNodes();

		int childOrder = 0;
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && !"poller".equals(child.getLocalName())) {
				BeanMetadataElement childBeanMetadata = this.parseChild(chainHandlerId, (Element) child, childOrder++,
						parserContext, builder.getBeanDefinition());
				if (childBeanMetadata instanceof RuntimeBeanReference) {
					String handlerBeanName = ((RuntimeBeanReference) childBeanMetadata).getBeanName();
					if (!handlerBeanNameSet.add(handlerBeanName)) {
						parserContext.getReaderContext().error("A bean definition is already registered for " +
										"beanName: '" + handlerBeanName + "' within the current <chain>.",
								element);
						return null;
					}
				}
				if ("gateway".equals(child.getLocalName())) {
					BeanDefinitionBuilder gwBuilder = BeanDefinitionBuilder.genericBeanDefinition(
							IntegrationContextUtils.BASE_PACKAGE + ".gateway.RequestReplyMessageHandlerAdapter");
					gwBuilder.addConstructorArgValue(childBeanMetadata);
					handlerList.add(gwBuilder.getBeanDefinition());
				}
				else {
					handlerList.add(childBeanMetadata);
				}
			}
		}
		builder.addPropertyValue("handlers", handlerList);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		return builder;
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
		String id = super.resolveId(element, definition, parserContext);
		BeanDefinition containingBeanDefinition = parserContext.getContainingBeanDefinition();
		if (containingBeanDefinition != null) {
			String nestedChainIdPrefix = (String) containingBeanDefinition.getAttribute(SI_CHAIN_NESTED_ID_ATTRIBUTE);
			if (StringUtils.hasText(nestedChainIdPrefix)) {
				id = nestedChainIdPrefix + "$child." + id;
			}
		}
		definition.setAttribute(SI_CHAIN_NESTED_ID_ATTRIBUTE, id);
		return id;
	}

	private BeanMetadataElement parseChild(String chainHandlerId, Element element, int order, ParserContext parserContext,
			BeanDefinition parentDefinition) {

		BeanDefinitionHolder holder = null;

		String id = element.getAttribute(ID_ATTRIBUTE);
		boolean hasId = StringUtils.hasText(id);
		String handlerComponentName = chainHandlerId + "$child" + (hasId ? "." + id : "#" + order);


		if ("bean".equals(element.getLocalName())) {
			holder = parserContext.getDelegate().parseBeanDefinitionElement(element, parentDefinition);
		}
		else {

			this.validateChild(element, parserContext);

			BeanDefinition beanDefinition = parserContext.getDelegate().parseCustomElement(element, parentDefinition);
			if (beanDefinition == null) {
				parserContext.getReaderContext().error("child BeanDefinition must not be null", element);
				return null;
			}
			else {
				holder = new BeanDefinitionHolder(beanDefinition, handlerComponentName + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX);
			}
		}

		holder.getBeanDefinition().getPropertyValues().add("componentName", handlerComponentName); // NOSONAR never null

		if (hasId) {
			BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
			return new RuntimeBeanReference(holder.getBeanName());
		}

		return holder;
	}

	private void validateChild(Element element, ParserContext parserContext) {

		final Object source = parserContext.extractSource(element);

		final String order = element.getAttribute(IntegrationNamespaceUtils.ORDER);

		if (StringUtils.hasText(order)) {
			parserContext.getReaderContext().error(IntegrationNamespaceUtils.createElementDescription(element) + " must not define " +
					"an 'order' attribute when used within a chain.", source);
		}

		final List<Element> pollerChildElements = DomUtils.getChildElementsByTagName(element, "poller");

		if (!pollerChildElements.isEmpty()) {
			parserContext.getReaderContext().error(IntegrationNamespaceUtils.createElementDescription(element) + " must not define " +
					"a 'poller' sub-element when used within a chain.", source);
		}
	}

}
