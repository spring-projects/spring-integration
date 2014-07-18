/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.xml.config;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.xml.result.DomResultFactory;
import org.springframework.integration.xml.result.ResultFactory;
import org.springframework.integration.xml.result.StringResultFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility methods for the XML namespace.
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Liujiong
 */
public abstract class XmlNamespaceUtils {

	public static final String DOM_RESULT = "DOMResult";

	public static final String STRING_RESULT = "StringResult";


	/**
	 * Helper method that encapsulates common logic for validating and building
	 * a bean definition for a {@link ResultFactory} based on either the
	 * 'result-factory' or 'result-type' attributes.
	 */
	public static ResultFactory configureResultFactory(String resultType, String resultFactoryName,BeanFactory beanFactory) {
		boolean bothHaveText = StringUtils.hasText(resultFactoryName) && StringUtils.hasText(resultType);
		ResultFactory resultFactory=null;
		Assert.state(!bothHaveText, "Only one of 'result-factory' or 'result-type' should be specified.");
		if (StringUtils.hasText(resultType)) {
			Assert.state(resultType.equals(DOM_RESULT) || resultType.equals(STRING_RESULT),
					"Result type must be either 'DOMResult' or 'StringResult'");
		}
		if (StringUtils.hasText(resultFactoryName)) {
			resultFactory=(ResultFactory) beanFactory.getBean(resultFactoryName);
		}
		else if (STRING_RESULT.equals(resultType)) {
			resultFactory=new StringResultFactory();
		}
		else if (DOM_RESULT.equals(resultType)) {
			resultFactory=new DomResultFactory();
		}
		return resultFactory;
	}

}
