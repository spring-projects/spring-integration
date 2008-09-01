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
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PriorityChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.RendezvousChannel;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;channel&gt; element.
 * 
 * @author Mark Fisher
 */
public class PointToPointChannelParser extends AbstractChannelParser {

	private static final String CAPACITY_ATTRIBUTE = "capacity";


	@Override
	protected BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = null;
		Element queueElement = null;
		if ((queueElement = DomUtils.getChildElementByTagName(element, "queue")) != null) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(QueueChannel.class);
			this.parseQueueCapacity(builder, queueElement);
		}
		else if ((queueElement = DomUtils.getChildElementByTagName(element, "priority-queue")) != null) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(PriorityChannel.class);
			this.parseQueueCapacity(builder, queueElement);
			String comparatorRef = queueElement.getAttribute("comparator");
			if (StringUtils.hasText(comparatorRef)) {
				builder.addConstructorArgReference(comparatorRef);
			}
		}
		else if ((queueElement = DomUtils.getChildElementByTagName(element, "rendezvous-queue")) != null) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(RendezvousChannel.class);
		}
		else {
			builder = BeanDefinitionBuilder.genericBeanDefinition(DirectChannel.class);
		}
		return builder;
	}

	private void parseQueueCapacity(BeanDefinitionBuilder builder, Element queueElement) {
		String capacity = queueElement.getAttribute(CAPACITY_ATTRIBUTE);
		if (StringUtils.hasText(capacity)) {
			if (!capacity.equals("UNBOUNDED")) {
				builder.addConstructorArgValue(Integer.valueOf(capacity));
			}
		}
	}

}
