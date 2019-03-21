/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.integration.webflux.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.http.config.HttpInboundEndpointParser;
import org.springframework.integration.webflux.inbound.WebFluxInboundEndpoint;

/**
 * @author Artem Bilan
 *
 * @since 5.0.1
 */
public class WebFluxInboundEndpointParser extends HttpInboundEndpointParser {

	public WebFluxInboundEndpointParser(boolean expectReply) {
		super(expectReply);
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return WebFluxInboundEndpoint.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "codec-configurer");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "requested-content-type-resolver");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reactive-adapter-registry");
	}

}
