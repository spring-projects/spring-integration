/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.integration.expression;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @since 3.0
 *
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
		assertNotNull(evalContext.getBeanResolver());
		assertNotNull(evalContext.getTypeConverter());
		IntegrationEvaluationContextFactoryBean factory = context.getBean("&" + IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				IntegrationEvaluationContextFactoryBean.class);
		assertSame(evalContext.getTypeConverter(), TestUtils.getPropertyValue(factory, "typeConverter"));
	}


	@Test
	public void testEvaluationContextDefaultTypeConverter() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				new RootBeanDefinition(IntegrationEvaluationContextFactoryBean.class));
		context.refresh();
		StandardEvaluationContext evalContext = ExpressionUtils.createStandardEvaluationContext(context);
		assertNotNull(evalContext.getBeanResolver());
		TypeConverter typeConverter = evalContext.getTypeConverter();
		assertNotNull(typeConverter);
		assertSame(TestUtils.getPropertyValue(typeConverter, "defaultConversionService"), TestUtils.getPropertyValue(typeConverter, "conversionService"));
	}

	@Test
	public void testEvaluationContextNoFactoryBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,
				new RootBeanDefinition(ConversionServiceFactoryBean.class));
		context.refresh();
		StandardEvaluationContext evalContext = ExpressionUtils.createStandardEvaluationContext(context);
		assertNotNull(evalContext.getBeanResolver());
		TypeConverter typeConverter = evalContext.getTypeConverter();
		assertNotNull(typeConverter);
		assertNotSame(TestUtils.getPropertyValue(typeConverter, "defaultConversionService"),
				TestUtils.getPropertyValue(typeConverter, "conversionService"));
		assertSame(context.getBean(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME),
				TestUtils.getPropertyValue(typeConverter, "conversionService"));
	}

	@Test
	public void testEvaluationContextNoBeanFactory() {
		StandardEvaluationContext evalContext = ExpressionUtils.createStandardEvaluationContext();
		assertNull(evalContext.getBeanResolver());
		TypeConverter typeConverter = evalContext.getTypeConverter();
		assertNotNull(typeConverter);
		assertSame(TestUtils.getPropertyValue(typeConverter, "defaultConversionService"),
				TestUtils.getPropertyValue(typeConverter, "conversionService"));
	}
}
