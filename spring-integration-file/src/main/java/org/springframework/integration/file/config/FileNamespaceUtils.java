/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.file.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class FileNamespaceUtils {

	public static void setDispositionAttributes(Element element, BeanDefinitionBuilder builder) {
		String dispositionExpression = element.getAttribute("disposition-expression");
		if (StringUtils.hasText(dispositionExpression)) {
			RootBeanDefinition expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(dispositionExpression);
			builder.addPropertyValue("dispositionExpression", expressionDef);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "disposition-result-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "disposition-send-timeout");
	}

}
