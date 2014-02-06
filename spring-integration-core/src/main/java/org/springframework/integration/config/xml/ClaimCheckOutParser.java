/*
 * Copyright 2002-2014 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.ClaimCheckOutTransformer;
import org.springframework.util.Assert;

/**
 * Parser for the &lt;claim-check-out/&gt; element.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ClaimCheckOutParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return ClaimCheckOutTransformer.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String messageStoreRef = element.getAttribute("message-store");
		Assert.hasText(messageStoreRef, "The 'message-store' attribute is required.");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "remove-message");
		builder.addConstructorArgReference(messageStoreRef);
	}

}
