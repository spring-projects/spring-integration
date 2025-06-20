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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.ClaimCheckInTransformer;
import org.springframework.util.Assert;

/**
 * Parser for the &lt;claim-check-in/&gt; element.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class ClaimCheckInParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return ClaimCheckInTransformer.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String messageStoreRef = element.getAttribute("message-store");
		Assert.hasText(messageStoreRef, "The 'message-store' attribute is required.");
		builder.addConstructorArgReference(messageStoreRef);
	}

}
