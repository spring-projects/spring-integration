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

package org.springframework.integration.xml.transformer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.integration.xml.result.DomResultFactory;
import org.springframework.integration.xml.result.ResultFactory;
import org.springframework.integration.xml.result.StringResultFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Super class for XML transformers.
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Liujiong
 * @author Gary Russell
 */
public abstract class AbstractXmlTransformer extends AbstractTransformer {

	public static final String DOM_RESULT = "DOMResult";

	public static final String STRING_RESULT = "StringResult";

	private volatile @Nullable String resultType;

	private volatile @Nullable String resultFactoryName;

	private volatile ResultFactory resultFactory = new DomResultFactory();

	public void setResultFactoryName(String resultFactoryName) {
		this.resultFactoryName = resultFactoryName;
	}

	public void setResultType(String resultType) {
		this.resultType = resultType;
	}

	public void setResultFactory(ResultFactory resultFactory) {
		Assert.notNull(resultFactory, "ResultFactory must not be null");
		this.resultFactory = resultFactory;
	}

	public @Nullable String getResultType() {
		return this.resultType;
	}

	public @Nullable String getResultFactoryName() {
		return this.resultFactoryName;
	}

	public ResultFactory getResultFactory() {
		return this.resultFactory;
	}

	@Override
	protected void onInit() {
		super.onInit();
		ResultFactory generatedResultFactory =
				configureResultFactory(this.resultType, this.resultFactoryName, getBeanFactory());
		if (generatedResultFactory != null) {
			this.resultFactory = generatedResultFactory;
		}
	}

	/**
	 * Helper method that encapsulates common logic for validating and building
	 * a bean definition for a {@link ResultFactory} based on either the
	 * 'result-factory' or 'result-type' attributes.
	 */
	private @Nullable ResultFactory configureResultFactory(@Nullable String resultType, @Nullable String resultFactoryName, BeanFactory beanFactory) {
		boolean bothHaveText = StringUtils.hasText(resultFactoryName) && StringUtils.hasText(resultType);
		ResultFactory configuredResultFactory = null;
		Assert.state(!bothHaveText, "Only one of 'result-factory' or 'result-type' should be specified.");
		if (StringUtils.hasText(resultType)) {
			Assert.state(resultType.equals(DOM_RESULT) || resultType.equals(STRING_RESULT),
					"Result type must be either 'DOMResult' or 'StringResult'");
		}
		if (StringUtils.hasText(resultFactoryName)) {
			configuredResultFactory = (ResultFactory) beanFactory.getBean(resultFactoryName);
		}
		else if (STRING_RESULT.equals(resultType)) {
			configuredResultFactory = new StringResultFactory();
		}
		else if (DOM_RESULT.equals(resultType)) {
			configuredResultFactory = new DomResultFactory();
		}
		return configuredResultFactory;
	}

}
