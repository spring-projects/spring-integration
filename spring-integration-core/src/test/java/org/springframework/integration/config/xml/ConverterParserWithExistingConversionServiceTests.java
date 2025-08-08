/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ConverterParserWithExistingConversionServiceTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	@Qualifier(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME)
	private ConversionService conversionService;

	@Test
	public void testConversionServiceAvailability() {
		assertThat(applicationContext.getBean(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME)
				.equals(conversionService)).isTrue();
		assertThat(conversionService.canConvert(TestBean1.class, TestBean2.class)).isTrue();
		assertThat(conversionService.canConvert(TestBean1.class, TestBean3.class)).isTrue();
	}

	@Test
	public void testParentConversionServiceAvailability() {
		ClassPathXmlApplicationContext parentContext = new ClassPathXmlApplicationContext(
				"ConverterParserWithExistingConversionServiceTests-parent.xml",
				ConverterParserWithExistingConversionServiceTests.class);
		GenericApplicationContext childContext = new GenericApplicationContext();
		childContext.setParent(parentContext);

		childContext.refresh();

		GenericConversionService conversionServiceParent =
				parentContext.getBean(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,
						GenericConversionService.class);
		GenericConversionService conversionServiceChild =
				childContext.getBean(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,
						GenericConversionService.class);
		assertThat(conversionServiceParent == conversionServiceChild).isTrue(); // validating that they are pointing to the same object
		conversionServiceChild.addConverter(new TestConverter());
		conversionServiceChild.addConverter(new TestConverter3());
		assertThat(conversionServiceChild.canConvert(TestBean1.class, TestBean2.class)).isTrue();
		assertThat(conversionServiceChild.canConvert(TestBean1.class, TestBean3.class)).isTrue();
		childContext.close();
		parentContext.close();
	}

	private static class TestBean1 {

		private final String text;

		@SuppressWarnings("unused")
		TestBean1(String text) {
			this.text = text;
		}

	}

	private static class TestBean2 {

		private final String text;

		TestBean2(String text) {
			this.text = text;
		}

		// called by router for channel name
		@Override
		public String toString() {
			return this.text.replace("-TEST", "_TARGET_CHANNEL");
		}

	}

	private static class TestBean3 {

		private final String text;

		TestBean3(String text) {
			this.text = text;
		}

		// called by router for channel name
		@Override
		public String toString() {
			return this.text.replace("-TEST", "_TARGET_CHANNEL");
		}

	}

	private static class TestConverter implements Converter<TestBean1, TestBean2> {

		TestConverter() {
			super();
		}

		@Override
		public TestBean2 convert(TestBean1 source) {
			return new TestBean2(source.text.toUpperCase());
		}

	}

	private static class TestConverter3 implements Converter<TestBean1, TestBean3> {

		TestConverter3() {
			super();
		}

		@Override
		public TestBean3 convert(TestBean1 source) {
			return new TestBean3(source.text.toUpperCase());
		}

	}

}
