/*
 * Copyright 2002-2010 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;


/**
 * @author Oleg Zhurakousky
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ConverterParserWithExistingConversionServiceTests {
	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	@Qualifier(IntegrationContextUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME)
	private ConversionService conversionService;

	@Test
	public void testConversionServiceAvailability(){
		Assert.isTrue(applicationContext.getBean(IntegrationContextUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME).equals(conversionService));
		Assert.isTrue(conversionService.canConvert(TestBean1.class, TestBean2.class));
		Assert.isTrue(conversionService.canConvert(TestBean1.class, TestBean3.class));
	}
	@Test
	public void testParentConversionServiceAvailability(){
		ApplicationContext parentContext =
			new ClassPathXmlApplicationContext("ConverterParserWithExistingConversionServiceTests-parent.xml", ConverterParserWithExistingConversionServiceTests.class);
		GenericApplicationContext childContext = new GenericApplicationContext();
		childContext.setParent(parentContext);
		childContext.refresh();

		GenericConversionService conversionServiceParent = parentContext.getBean(IntegrationContextUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,GenericConversionService.class);
		GenericConversionService conversionServiceChild = childContext.getBean(IntegrationContextUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,GenericConversionService.class);
		Assert.isTrue(conversionServiceParent == conversionServiceChild); // validating that they are pointing to the same object
		conversionServiceChild.addConverter(new TestConverter());
		conversionServiceChild.addConverter(new TestConverter3());
		Assert.isTrue(conversionServiceChild.canConvert(TestBean1.class, TestBean2.class));
		Assert.isTrue(conversionServiceChild.canConvert(TestBean1.class, TestBean3.class));
	}


	private static class TestBean1  {

		private String text;

		@SuppressWarnings("unused")
		public TestBean1(String text) {
			this.text = text;
		}
	}


	private static class TestBean2 {

		private String text;

		public TestBean2(String text) {
			this.text = text;
		}

		// called by router for channel name
		public String toString() {
			return this.text.replace("-TEST", "_TARGET_CHANNEL");
		}
	}
	private static class TestBean3 {

		private String text;

		public TestBean3(String text) {
			this.text = text;
		}

		// called by router for channel name
		public String toString() {
			return this.text.replace("-TEST", "_TARGET_CHANNEL");
		}
	}

	private static class TestConverter implements Converter<TestBean1, TestBean2> {

		public TestBean2 convert(TestBean1 source) {
			return new TestBean2(source.text.toUpperCase());
		}
	}

	private static class TestConverter3 implements Converter<TestBean1, TestBean3> {

		public TestBean3 convert(TestBean1 source) {
			return new TestBean3(source.text.toUpperCase());
		}
	}
}
