/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.dsl;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscription;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperty;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.mqtt.client.core.Mqtt3ClientManager;
import org.springframework.integration.mqtt.client.core.Mqtt5ClientManager;
import org.springframework.integration.mqtt.client.inbound.AbstractMqttMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.client.outbound.AbstractMqttMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author YeongJae Min
 *
 * @since 7.3
 */
class MqttSpecTests {

	@Test
	void mqtt3ExpressionsFunctionsFallbacksAndConverter() {
		Mqtt3ClientManager manager = mock();
		Mqtt3Client client = mock();
		Mqtt3BlockingClient blockingClient = mock();
		given(manager.getClient()).willReturn(client);
		given(client.toBlocking()).willReturn(blockingClient);
		MessageConverter converter = mock();
		given(converter.toMessage(any(), any()))
				.willAnswer(invocation -> MessageBuilder.withPayload("converted").build());
		var parser = new SpelExpressionParser();
		var spec = Mqtt3.outboundAdapter(manager)
				.defaultTopic("dsl/default").defaultQos(MqttQos.EXACTLY_ONCE).defaultRetained(true)
				.messageConverter(converter)
				.topicExpression(parser.parseExpression("headers['destination']"))
				.qosExpression("headers['quality']")
				.retainedFunction(message -> message.getHeaders().get("retain", Boolean.class));
		var handler = spec.getObject();
		initialize(handler);
		Message<?> message = MessageBuilder.withPayload(42)
				.setHeader("destination", "dsl/dynamic")
				.setHeader("quality", MqttQos.AT_LEAST_ONCE)
				.setHeader("retain", false)
				.build();
		handler.handleMessage(message);
		spec.topicFunction(request -> null).qosFunction(request -> null).retainedFunction(request -> null);
		handler.handleMessage(message);
		ArgumentCaptor<Mqtt3Publish> publish = ArgumentCaptor.forClass(Mqtt3Publish.class);
		verify(blockingClient, times(2)).publish(publish.capture());
		assertThat(publish.getAllValues()).containsExactly(
				Mqtt3Publish.builder().topic("dsl/dynamic").qos(MqttQos.AT_LEAST_ONCE)
						.retain(false).payload("converted".getBytes()).build(),
				Mqtt3Publish.builder().topic("dsl/default").qos(MqttQos.EXACTLY_ONCE)
						.retain(true).payload("converted".getBytes()).build());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void mqtt5StringAndExpressionProperties(boolean stringExpressions) {
		var parser = new SpelExpressionParser();
		Mqtt5ClientManager manager = mock();
		Mqtt5Client client = mock();
		Mqtt5BlockingClient blockingClient = mock();
		given(manager.getClient()).willReturn(client);
		given(client.toBlocking()).willReturn(blockingClient);
		var spec = Mqtt5.outboundAdapter(manager)
				.defaultTopic("dsl/properties").defaultRetained(true)
				.qosExpression(parser.parseExpression("headers['quality']"))
				.retainedExpression(parser.parseExpression("false"));
		if (stringExpressions) {
			spec.contentTypeExpression("'text/plain'")
					.correlationDataExpression("headers['correlation']")
					.responseTopicExpression("'dsl/replies'")
					.messageExpiryIntervalExpression("60L")
					.payloadFormatIndicatorExpression("headers['format']")
					.userPropertiesExpression("headers['properties']");
		}
		else {
			spec.contentTypeExpression(parser.parseExpression("'text/plain'"))
					.correlationDataExpression(parser.parseExpression("headers['correlation']"))
					.responseTopicExpression(parser.parseExpression("'dsl/replies'"))
					.messageExpiryIntervalExpression(parser.parseExpression("60L"))
					.payloadFormatIndicatorExpression(parser.parseExpression("headers['format']"))
					.userPropertiesExpression(parser.parseExpression("headers['properties']"));
		}
		var handler = spec.getObject();
		initialize(handler);
		Message<?> message = MessageBuilder.withPayload("properties")
				.setHeader("quality", MqttQos.AT_LEAST_ONCE)
				.setHeader("correlation", new byte[] {3, 4})
				.setHeader("format", Mqtt5PayloadFormatIndicator.UTF_8)
				.setHeader("properties", Map.of("device", "meter"))
				.build();
		handler.handleMessage(message);
		Function<Message<String>, Map<String, String>> properties = request -> Map.of("source", "function");
		spec.userPropertiesFunction(properties);
		handler.handleMessage(message);
		ArgumentCaptor<Mqtt5Publish> publishes = ArgumentCaptor.forClass(Mqtt5Publish.class);
		verify(blockingClient, times(2)).publish(publishes.capture());
		assertThat(publishes.getAllValues()).allSatisfy(publish -> {
			assertThat(publish.getContentType()).hasValueSatisfying(value ->
					assertThat(value.toString()).isEqualTo("text/plain"));
			assertThat(publish.getCorrelationData()).hasValue(ByteBuffer.wrap(new byte[] {3, 4}));
			assertThat(publish.getResponseTopic()).hasValueSatisfying(value ->
					assertThat(value.toString()).isEqualTo("dsl/replies"));
			assertThat(publish.getMessageExpiryInterval()).hasValue(60L);
			assertThat(publish.getPayloadFormatIndicator()).hasValue(Mqtt5PayloadFormatIndicator.UTF_8);
			assertThat(publish.getQos()).isEqualTo(MqttQos.AT_LEAST_ONCE);
			assertThat(publish.isRetain()).isFalse();
		});
		assertThat(publishes.getAllValues().get(0).getUserProperties().asList())
				.isEqualTo(List.of(Mqtt5UserProperty.of("device", "meter")));
		assertThat(publishes.getAllValues().get(1).getUserProperties().asList())
				.isEqualTo(List.of(Mqtt5UserProperty.of("source", "function")));
	}

	@Test
	void nativePublishesBypassExpressionsDefaultsAndConversion() {
		Expression expression = mock();
		MessageConverter converter = mock();
		Mqtt3ClientManager manager3 = mock();
		Mqtt3Client client3 = mock();
		Mqtt3BlockingClient blocking3 = mock();
		given(manager3.getClient()).willReturn(client3);
		given(client3.toBlocking()).willReturn(blocking3);
		var handler3 = Mqtt3.outboundAdapter(manager3)
				.defaultTopic("dsl/unused").defaultQos(MqttQos.AT_MOST_ONCE).defaultRetained(false)
				.topicExpression(expression).qosExpression(expression).retainedExpression(expression)
				.messageConverter(converter).getObject();
		initialize(handler3);
		Mqtt3Publish publish3 = Mqtt3Publish.builder().topic("dsl/native/v3")
				.qos(MqttQos.EXACTLY_ONCE).retain(true).payload(new byte[] {1}).build();
		handler3.handleMessage(MessageBuilder.withPayload(publish3).build());
		ArgumentCaptor<Mqtt3Publish> captured3 = ArgumentCaptor.forClass(Mqtt3Publish.class);
		verify(blocking3).publish(captured3.capture());
		assertThat(captured3.getValue()).isSameAs(publish3);

		Mqtt5ClientManager manager5 = mock();
		Mqtt5Client client5 = mock();
		Mqtt5BlockingClient blocking5 = mock();
		given(manager5.getClient()).willReturn(client5);
		given(client5.toBlocking()).willReturn(blocking5);
		var handler5 = Mqtt5.outboundAdapter(manager5)
				.defaultTopic("dsl/unused").defaultQos(MqttQos.AT_MOST_ONCE).defaultRetained(false)
				.topicExpression(expression).qosExpression(expression).retainedExpression(expression)
				.contentTypeExpression(expression).correlationDataExpression(expression)
				.responseTopicExpression(expression).messageExpiryIntervalExpression(expression)
				.payloadFormatIndicatorExpression(expression).userPropertiesExpression(expression)
				.messageConverter(converter).getObject();
		initialize(handler5);
		Mqtt5Publish publish5 = Mqtt5Publish.builder().topic("dsl/native/v5")
				.qos(MqttQos.EXACTLY_ONCE).retain(true).payload(new byte[] {2}).contentType("application/json")
				.build();
		handler5.handleMessage(MessageBuilder.withPayload(publish5).build());
		ArgumentCaptor<Mqtt5Publish> captured5 = ArgumentCaptor.forClass(Mqtt5Publish.class);
		verify(blocking5).publish(captured5.capture());
		assertThat(captured5.getValue()).isSameAs(publish5);
		verifyNoInteractions(expression, converter);
	}

	@Test
	void explicitSubscriptionsRemainAuthoritative() {
		Mqtt3ClientManager manager3 = mock();
		given(manager3.getClient()).willReturn(mock());
		Mqtt3Subscription subscription3 = Mqtt3Subscription.builder()
				.topicFilter("dsl/explicit/v3").qos(MqttQos.AT_MOST_ONCE).build();
		var adapter3 = Mqtt3.inboundAdapter(manager3, subscription3).qos(MqttQos.EXACTLY_ONCE).getObject();
		initialize(adapter3);
		assertThat(TestUtils.<List<?>>getPropertyValue(adapter3, "subscriptions")).isEqualTo(List.of(subscription3));

		Mqtt5ClientManager manager5 = mock();
		given(manager5.getClient()).willReturn(mock());
		Mqtt5Subscription subscription5 = Mqtt5Subscription.builder().topicFilter("dsl/explicit/v5")
				.qos(MqttQos.EXACTLY_ONCE).noLocal(true).retainHandling(Mqtt5RetainHandling.SEND)
				.retainAsPublished(false).build();
		var adapter5 = Mqtt5.inboundAdapter(manager5, subscription5).qos(MqttQos.AT_MOST_ONCE)
				.noLocal(false).retainHandling(Mqtt5RetainHandling.DO_NOT_SEND).retainAsPublished(true).getObject();
		initialize(adapter5);
		assertThat(TestUtils.<List<?>>getPropertyValue(adapter5, "subscriptions")).isEqualTo(List.of(subscription5));
	}

	@Test
	void topicSubscriptionsUseFluentOptions() {
		Mqtt5ClientManager manager = mock();
		given(manager.getClient()).willReturn(mock());
		var adapter = Mqtt5.inboundAdapter(manager, "dsl/topics").qos(MqttQos.EXACTLY_ONCE)
				.noLocal(true).retainHandling(Mqtt5RetainHandling.DO_NOT_SEND).retainAsPublished(true).getObject();
		initialize(adapter);
		assertThat(TestUtils.<List<?>>getPropertyValue(adapter, "subscriptions")).isEqualTo(List.of(
				Mqtt5Subscription.builder().topicFilter("dsl/topics").qos(MqttQos.EXACTLY_ONCE)
						.noLocal(true).retainHandling(Mqtt5RetainHandling.DO_NOT_SEND).retainAsPublished(true).build()));
	}

	private static void initialize(AbstractMqttMessageHandler<?> handler) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				new StandardEvaluationContext());
		beanFactory.registerSingleton(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME,
				new StringMessageConverter());
		handler.setBeanFactory(beanFactory);
		handler.afterPropertiesSet();
	}

	private static void initialize(AbstractMqttMessageDrivenChannelAdapter<?> adapter) {
		adapter.setBeanFactory(new DefaultListableBeanFactory());
		adapter.setMessageConverter(new StringMessageConverter());
		adapter.afterPropertiesSet();
	}

}
