/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.converter.DefaultDatatypeChannelMessageConverter;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.0
 */
public class DatatypeChannelTests {

	@Test
	public void supportedType() {
		MessageChannel channel = createChannel(String.class);
		assertThat(channel.send(new GenericMessage<String>("test"))).isTrue();
	}

	@Test
	public void unsupportedTypeAndNoConversionService() {
		MessageChannel channel = createChannel(Integer.class);
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> channel.send(new GenericMessage<String>("123")));
	}

	@Test
	public void unsupportedTypeButConversionServiceSupports() {
		QueueChannel channel = createChannel(Integer.class);
		ConversionService conversionService = new DefaultConversionService();
		DefaultDatatypeChannelMessageConverter converter = new DefaultDatatypeChannelMessageConverter();
		converter.setConversionService(conversionService);
		channel.setMessageConverter(converter);
		assertThat(channel.send(new GenericMessage<String>("123"))).isTrue();
	}

	@Test
	public void unsupportedTypeAndConversionServiceDoesNotSupport() {
		QueueChannel channel = createChannel(Integer.class);
		ConversionService conversionService = new DefaultConversionService();
		DefaultDatatypeChannelMessageConverter converter = new DefaultDatatypeChannelMessageConverter();
		converter.setConversionService(conversionService);
		channel.setMessageConverter(converter);
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> assertThat(channel.send(new GenericMessage<Boolean>(Boolean.TRUE))).isTrue());
	}

	@Test
	public void unsupportedTypeButCustomConversionServiceSupports() {
		QueueChannel channel = createChannel(Integer.class);
		GenericConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new Converter<Boolean, Integer>() {

			@Override
			public Integer convert(Boolean source) {
				return source ? 1 : 0;
			}
		});
		DefaultDatatypeChannelMessageConverter converter = new DefaultDatatypeChannelMessageConverter();
		converter.setConversionService(conversionService);
		channel.setMessageConverter(converter);
		assertThat(channel.send(new GenericMessage<Boolean>(Boolean.TRUE))).isTrue();
		assertThat(channel.receive().getPayload()).isEqualTo(1);
	}

	@Test
	public void conversionServiceBeanUsedByDefault() {
		GenericApplicationContext context = new GenericApplicationContext();
		Converter<Boolean, Integer> converter = new Converter<Boolean, Integer>() {

			@Override
			public Integer convert(Boolean source) {
				return source ? 1 : 0;
			}
		};
		BeanDefinitionBuilder conversionServiceBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(ConversionServiceFactoryBean.class);
		conversionServiceBuilder.addPropertyValue("converters", Collections.singleton(converter));
		context.registerBeanDefinition(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,
				conversionServiceBuilder.getBeanDefinition());
		BeanDefinition messageConverter = BeanDefinitionBuilder.genericBeanDefinition(
				DefaultDatatypeChannelMessageConverter.class).getBeanDefinition();
		context.registerBeanDefinition(
				IntegrationContextUtils.INTEGRATION_DATATYPE_CHANNEL_MESSAGE_CONVERTER_BEAN_NAME,
				messageConverter);
		BeanDefinitionBuilder channelBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(QueueChannel.class);
		channelBuilder.addPropertyValue("datatypes", "java.lang.Integer, java.util.Date");
		context.registerBeanDefinition("testChannel", channelBuilder.getBeanDefinition());
		context.refresh();

		QueueChannel channel = context.getBean("testChannel", QueueChannel.class);
		assertThat(TestUtils.getPropertyValue(channel, "messageConverter.conversionService"))
				.isSameAs(context.getBean(ConversionService.class));
		assertThat(channel.send(new GenericMessage<Boolean>(Boolean.TRUE))).isTrue();
		assertThat(channel.receive().getPayload()).isEqualTo(1);
		context.close();
	}

	@Test
	public void conversionServiceReferenceOverridesDefault() {
		GenericApplicationContext context = new GenericApplicationContext();
		Converter<Boolean, Integer> defaultConverter = new Converter<Boolean, Integer>() {

			@Override
			public Integer convert(Boolean source) {
				return source ? 1 : 0;
			}
		};
		GenericConversionService customConversionService = new DefaultConversionService();
		customConversionService.addConverter(new Converter<Boolean, Integer>() {

			@Override
			public Integer convert(Boolean source) {
				return source ? 99 : -99;
			}
		});
		BeanDefinitionBuilder conversionServiceBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(ConversionServiceFactoryBean.class);
		conversionServiceBuilder.addPropertyValue("converters", Collections.singleton(defaultConverter));
		context.registerBeanDefinition("conversionService", conversionServiceBuilder.getBeanDefinition());
		BeanDefinitionBuilder messageConverterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				DefaultDatatypeChannelMessageConverter.class);
		messageConverterBuilder.addPropertyValue("conversionService", customConversionService);
		context.registerBeanDefinition(
				IntegrationContextUtils.INTEGRATION_DATATYPE_CHANNEL_MESSAGE_CONVERTER_BEAN_NAME,
				messageConverterBuilder.getBeanDefinition());
		BeanDefinitionBuilder channelBuilder = BeanDefinitionBuilder.genericBeanDefinition(QueueChannel.class);
		channelBuilder.addPropertyValue("datatypes", "java.lang.Integer, java.util.Date");
		context.registerBeanDefinition("testChannel", channelBuilder.getBeanDefinition());
		context.refresh();

		QueueChannel channel = context.getBean("testChannel", QueueChannel.class);
		assertThat(channel.send(new GenericMessage<Boolean>(Boolean.TRUE))).isTrue();
		assertThat(channel.receive().getPayload()).isEqualTo(99);
		context.close();
	}

	@Test
	public void multipleTypes() {
		MessageChannel channel = createChannel(String.class, Integer.class);
		assertThat(channel.send(new GenericMessage<String>("test1"))).isTrue();
		assertThat(channel.send(new GenericMessage<Integer>(2))).isTrue();
		Exception exception = null;
		try {
			channel.send(new GenericMessage<Date>(new Date()));
		}
		catch (MessageDeliveryException e) {
			exception = e;
		}
		assertThat(exception).isNotNull();
	}

	@Test
	public void subclassOfAcceptedType() {
		MessageChannel channel = createChannel(RuntimeException.class);
		assertThat(channel.send(new ErrorMessage(new MessagingException("test")))).isTrue();
	}

	@Test
	public void superclassOfAcceptedTypeNotAccepted() {
		MessageChannel channel = createChannel(RuntimeException.class);
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> channel.send(new ErrorMessage(new Exception("test"))));
	}

	@Test
	public void genericConverters() {
		QueueChannel channel = createChannel(Foo.class);
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new StringToBarConverter());
		conversionService.addConverter(new IntegerToBazConverter());
		DefaultDatatypeChannelMessageConverter converter = new DefaultDatatypeChannelMessageConverter();
		converter.setConversionService(conversionService);
		channel.setMessageConverter(converter);
		assertThat(channel.send(new GenericMessage<String>("foo"))).isTrue();
		Message<?> out = channel.receive(0);
		assertThat(out.getPayload()).isInstanceOf(Bar.class);
		assertThat(channel.send(new GenericMessage<Integer>(42))).isTrue();
		out = channel.receive(0);
		assertThat(out.getPayload()).isInstanceOf(Baz.class);
	}

	private static QueueChannel createChannel(Class<?>... datatypes) {
		QueueChannel channel = new QueueChannel();
		channel.setBeanName("testChannel");
		channel.setDatatypes(datatypes);
		return channel;
	}

	private static class Foo {

		Foo() {
			super();
		}

	}

	private static class Bar extends Foo {

		Bar() {
			super();
		}

	}

	private static class Baz extends Foo {

		Baz() {
			super();
		}

	}

	private static class StringToBarConverter implements GenericConverter {

		StringToBarConverter() {
			super();
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			Set<ConvertiblePair> pairs = new HashSet<ConvertiblePair>();
			pairs.add(new ConvertiblePair(String.class, Foo.class));
			pairs.add(new ConvertiblePair(String.class, Bar.class));
			return pairs;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return new Bar();
		}

	}

	private static class IntegerToBazConverter implements GenericConverter {

		IntegerToBazConverter() {
			super();
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			Set<ConvertiblePair> pairs = new HashSet<ConvertiblePair>();
			pairs.add(new ConvertiblePair(Integer.class, Foo.class));
			pairs.add(new ConvertiblePair(Integer.class, Baz.class));
			return pairs;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return new Baz();
		}

	}

}
