/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jdbc.store.JdbcMessageStore;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.util.StringUtils;

/**
 * Parser for {@link JdbcMessageStore}.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.0
 */
public class JdbcMessageStoreParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JdbcMessageStore.class);

		String dataSourceRef = element.getAttribute("data-source");
		String simpleJdbcOperationsRef = element.getAttribute("jdbc-operations");
		boolean refToDataSourceSet = StringUtils.hasText(dataSourceRef);
		boolean refToSimpleJdbcOperationsSet = StringUtils.hasText(simpleJdbcOperationsRef);
		if ((refToDataSourceSet && refToSimpleJdbcOperationsSet)
				|| (!refToDataSourceSet && !refToSimpleJdbcOperationsSet)) {
			parserContext.getReaderContext().error(
					"Exactly one of the attributes data-source or "
							+ "simple-jdbc-operations should be set for the JDBC message-store", source);
		}

		builder.addConstructorArgReference(refToDataSourceSet ? dataSourceRef : simpleJdbcOperationsRef);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "serializer");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "deserializer");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "table-prefix");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "region");

		String allowedPatterns = element.getAttribute("allowed-patterns");
		String allowedPatternsOverride = element.getAttribute("allowed-patterns-override");
		boolean allowedPatternsSet = StringUtils.hasText(allowedPatterns);
		boolean allowedPatternsOverrideSet = StringUtils.hasText(allowedPatternsOverride);
		if (allowedPatternsSet && allowedPatternsOverrideSet) {
			parserContext.getReaderContext().error(
					"At most one of the attributes allowed-patterns or allowed-patterns-override " +
							"should be set for the JDBC message-store", source);
		}
		if (allowedPatternsSet) {
			// The attribute adds to the defaults, while 'setAllowedPatterns()' replaces them
			String[] patterns = StringUtils.tokenizeToStringArray(allowedPatterns, ",");
			builder.addPropertyValue("allowedPatterns", StringUtils.concatenateStringArrays(
					AllowListDeserializingConverter.DEFAULT_ALLOWED_PATTERNS.toArray(new String[0]), patterns));
		}
		else if (allowedPatternsOverrideSet) {
			builder.addPropertyValue("allowedPatterns",
					StringUtils.tokenizeToStringArray(allowedPatternsOverride, ","));
		}

		return builder.getBeanDefinition();

	}

}
