/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Objects;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * Parser for the top level 'wire-tap' element.
 *
 * @author David Turanski
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.1
 *
 */
public class GlobalWireTapParser extends GlobalChannelInterceptorParser {

	@Override
	protected Object getBeanDefinitionBuilderConstructorValue(Element element, ParserContext parserContext) {
		String wireTapBeanName = new WireTapParser().parse(element, parserContext);
		return new RuntimeBeanReference(wireTapBeanName);
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		RuntimeBeanReference wireTapBean =
				(RuntimeBeanReference) definition.getConstructorArgumentValues()
						.getIndexedArgumentValues()
						.values()
						.iterator()
						.next()
						.getValue();
		return Objects.requireNonNull(wireTapBean).getBeanName() + ".globalChannelInterceptor";
	}

}
