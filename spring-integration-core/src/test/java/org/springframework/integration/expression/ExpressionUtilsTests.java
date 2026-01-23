/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.integration.expression;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 3.0
 */
public class ExpressionUtilsTests {

	@Test
	public void testEvaluationContext() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				new RootBeanDefinition(IntegrationEvaluationContextFactoryBean.class));
		context.registerBeanDefinition(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,
				new RootBeanDefinition(ConversionServiceFactoryBean.class));
		context.refresh();
		StandardEvaluationContext evalContext = ExpressionUtils.createStandardEvaluationContext(context);
		assertThat(evalContext.getBeanResolver()).isNotNull();
		assertThat(evalContext.getTypeConverter()).isNotNull();
		IntegrationEvaluationContextFactoryBean factory =
				context.getBean("&" + IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
						IntegrationEvaluationContextFactoryBean.class);
		assertThat(TestUtils.<Object>getPropertyValue(factory, "typeConverter"))
				.isSameAs(evalContext.getTypeConverter());
	}

	@Test
	public void testEvaluationContextDefaultTypeConverter() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				new RootBeanDefinition(IntegrationEvaluationContextFactoryBean.class));
		context.refresh();
		StandardEvaluationContext evalContext = ExpressionUtils.createStandardEvaluationContext(context);
		assertThat(evalContext.getBeanResolver()).isNotNull();
		TypeConverter typeConverter = evalContext.getTypeConverter();
		assertThat(typeConverter).isNotNull();
		assertThat(TestUtils.<Supplier<?>>getPropertyValue(typeConverter, "conversionService").get())
				.isSameAs(DefaultConversionService.getSharedInstance());
	}

	@Test
	public void testEvaluationContextNoFactoryBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,
				new RootBeanDefinition(ConversionServiceFactoryBean.class));
		context.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				new RootBeanDefinition(StandardEvaluationContext.class));
		context.refresh();
		StandardEvaluationContext evalContext = ExpressionUtils.createStandardEvaluationContext(context);
		assertThat(evalContext.getBeanResolver()).isNull();
	}

	@Test
	public void testEvaluationContextNoBeanFactory() {
		StandardEvaluationContext evalContext = ExpressionUtils.createStandardEvaluationContext();
		assertThat(evalContext.getBeanResolver()).isNull();
		TypeConverter typeConverter = evalContext.getTypeConverter();
		assertThat(typeConverter).isNotNull();
		assertThat(TestUtils.<Supplier<?>>getPropertyValue(typeConverter, "conversionService").get())
				.isSameAs(DefaultConversionService.getSharedInstance());
	}

}
