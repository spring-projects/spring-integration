/*
 * Copyright 2002-2024 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ControlBusFactoryBean;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ControlBusParser extends AbstractConsumerEndpointParser {

	@Override
	@SuppressWarnings("removal")
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ControlBusFactoryBean.class);
		if (Boolean.FALSE.equals(Boolean.parseBoolean(element.getAttribute("use-registry")))) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(
					org.springframework.integration.config.ExpressionControlBusFactoryBean.class);
			parserContext.getReaderContext()
					.warning("The 'ExpressionControlBusFactoryBean' for '<control-bus>' is deprecated (for removal) " +
							"in favor of 'ControlBusFactoryBean'. " +
							"Set 'use-registry' attribute to 'true' to switch to a new functionality.",
							element);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "order");
		return builder;
	}

}
