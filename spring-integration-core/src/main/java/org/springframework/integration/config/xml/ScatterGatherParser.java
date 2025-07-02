/*
 * Copyright 2014-present the original author or authors.
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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.integration.scattergather.ScatterGatherHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;scatter-gather&gt; element.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.1
 */
public class ScatterGatherParser extends AbstractConsumerEndpointParser {

	private static final RecipientListRouterParser SCATTERER_PARSER = new RecipientListRouterParser();

	private static final AggregatorParser GATHERER_PARSER = new AggregatorParser();

	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		String scatterChannel = element.getAttribute("scatter-channel");
		boolean hasScatterChannel = StringUtils.hasText(scatterChannel);
		Element scatterer = DomUtils.getChildElementByTagName(element, "scatterer");
		boolean hasScatterer = scatterer != null;

		if (hasScatterChannel & hasScatterer) {
			parserContext.getReaderContext()
					.error("'scatter-channel' attribute and 'scatterer' sub-element are mutually exclusive", element);
		}

		if (!hasScatterChannel & !hasScatterer) {
			parserContext.getReaderContext()
					.error("The 'scatter-channel' attribute or 'scatterer' sub-element must be specified", element);
		}

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ScatterGatherHandler.class);
		AbstractBeanDefinition scatterGatherDefinition = builder.getRawBeanDefinition();
		String id = resolveId(element, scatterGatherDefinition, parserContext);

		scatter(parserContext, scatterChannel, hasScatterChannel, scatterer, hasScatterer, builder,
				scatterGatherDefinition, id);

		gather(element, parserContext, builder, scatterGatherDefinition, id);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "gather-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "gather-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");

		return builder;
	}

	private void scatter(ParserContext parserContext, String scatterChannel, boolean hasScatterChannel,
			@Nullable Element scatterer, boolean hasScatterer, BeanDefinitionBuilder builder,
			AbstractBeanDefinition scatterGatherDefinition, String id) {

		if (hasScatterChannel) {
			builder.addConstructorArgReference(scatterChannel);
		}
		else {
			BeanDefinition scattererDefinition;
			if (!hasScatterer) {
				scattererDefinition = new RootBeanDefinition(RecipientListRouter.class);
			}
			else {
				scattererDefinition = SCATTERER_PARSER.parse(Objects.requireNonNull(scatterer),
						new ParserContext(parserContext.getReaderContext(), parserContext.getDelegate(),
								scatterGatherDefinition));
			}
			Assert.state(scatterer != null, "scatterer must not be null");
			String scattererId = id + ".scatterer";
			if (hasScatterer && scatterer.hasAttribute(ID_ATTRIBUTE)) {
				scattererId = scatterer.getAttribute(ID_ATTRIBUTE);
			}
			Assert.state(scattererDefinition != null, "scattererDefinition must not be null");

			if (!scatterer.hasAttribute("apply-sequence")) {
				scattererDefinition.getPropertyValues().addPropertyValue("applySequence", true);
			}

			parserContext.getRegistry().registerBeanDefinition(scattererId, scattererDefinition); // NOSONAR not null
			builder.addConstructorArgValue(new RuntimeBeanReference(scattererId));
		}
	}

	private void gather(Element element, ParserContext parserContext, BeanDefinitionBuilder builder,
			AbstractBeanDefinition scatterGatherDefinition, String id) {

		Element gatherer = DomUtils.getChildElementByTagName(element, "gatherer");

		BeanDefinition gathererDefinition;
		if (gatherer == null) {
			try {
				gatherer = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder().newDocument().createElement("aggregator");
			}
			catch (ParserConfigurationException e) {
				parserContext.getReaderContext().error(
						e.getMessage() == null ? "Invalid Parser Configuration" : e.getMessage(), element);
			}
		}
		Assert.state(gatherer != null, "gatherer must not be null");
		gathererDefinition = GATHERER_PARSER.parse(gatherer, // NOSONAR
				new ParserContext(parserContext.getReaderContext(),
						parserContext.getDelegate(), scatterGatherDefinition));
		String gathererId = id + ".gatherer";
		if (gatherer.hasAttribute(ID_ATTRIBUTE)) {
			gathererId = gatherer.getAttribute(ID_ATTRIBUTE);
		}
		parserContext.getRegistry().registerBeanDefinition(gathererId, Objects.requireNonNull(gathererDefinition));
		builder.addConstructorArgValue(new RuntimeBeanReference(gathererId));
	}

}
