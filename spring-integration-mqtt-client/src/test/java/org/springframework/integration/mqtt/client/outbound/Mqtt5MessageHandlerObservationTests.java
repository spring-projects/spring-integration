/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.outbound;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.integration.mqtt.client.support.MqttHeaderMapper;
import org.springframework.integration.mqtt.client.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Hyunah Oh
 *
 * @since 7.3
 */
class Mqtt5MessageHandlerObservationTests {

	private final MqttObservationTestSupport tracing = new MqttObservationTestSupport();

	private final AtomicReference<Mqtt5Publish> published = new AtomicReference<>();

	private final CompletableFuture<Mqtt5PublishResult> publishFuture = new CompletableFuture<>();

	private final Mqtt5BlockingClient blockingClient = mock();

	private final Mqtt5AsyncClient asyncClient = mock();

	private AnnotationConfigApplicationContext applicationContext;

	private Mqtt5MessageHandler handler;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		this.applicationContext = new AnnotationConfigApplicationContext(Config.class);
		Mqtt5Client client = mock();
		when(client.toBlocking()).thenReturn(this.blockingClient);
		when(client.toAsync()).thenReturn(this.asyncClient);
		when(this.blockingClient.publish(any(Mqtt5Publish.class))).thenAnswer(invocation -> {
			this.published.set(invocation.getArgument(0));
			return mock(Mqtt5PublishResult.class);
		});
		when(this.asyncClient.publish(any(Mqtt5Publish.class))).thenAnswer(invocation -> {
			this.published.set(invocation.getArgument(0));
			return this.publishFuture;
		});
		ClientManager<Mqtt5Client> manager = mock();
		when(manager.getClient()).thenReturn(client);
		this.handler = new Mqtt5MessageHandler(manager);
		this.handler.setBeanName("temperaturePublisher");
		this.handler.setBeanFactory(this.applicationContext.getBeanFactory());
		this.handler.setApplicationEventPublisher(event -> { });
		this.handler.setMessageConverter(new SimpleMessageConverter());
		this.handler.registerObservationRegistry(this.tracing.registry);
		this.handler.afterPropertiesSet();
	}

	@AfterEach
	void tearDown() {
		this.applicationContext.close();
		this.tracing.close();
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	@SuppressWarnings("try")
	void propagatesProducerSpanAndPreservesUserProperties(boolean async) {
		this.handler.setAsync(async);
		Span parent = this.tracing.tracer.nextSpan().name("temperature-request").start();
		Message<?> message = MessageBuilder.withPayload("temperature=21")
				.setHeader(MqttHeaders.TOPIC, "sensors/temperature")
				.setHeader(MqttHeaders.USER_PROPERTIES, Map.of("sensor-id", "sensor-42")).build();
		try (Tracer.SpanInScope ignored = this.tracing.tracer.withSpan(parent)) {
			this.handler.handleMessage(message);
			this.publishFuture.complete(mock(Mqtt5PublishResult.class));
			SpanData producer = producerSpan();
			assertThat(producer.getTraceId()).isEqualTo(parent.context().traceId());
			SpanData consumer = this.tracing.spans.stream()
					.filter(span -> span.getKind() == SpanKind.CONSUMER).findFirst().orElseThrow();
			assertThat(producer.getParentSpanId()).isEqualTo(consumer.getSpanId());
			assertThat(new MqttHeaderMapper().toHeaders(this.published.get()))
					.containsEntry("sensor-id", "sensor-42");
			assertThat(new MqttHeaderMapper().toHeaders(this.published.get()).get("traceparent").toString())
					.startsWith("00-" + producer.getTraceId() + "-" + producer.getSpanId() + "-");
			assertThat(message.getHeaders().get(MqttHeaders.USER_PROPERTIES))
					.isEqualTo(Map.of("sensor-id", "sensor-42"));
			assertThat(this.tracing.tracer.currentSpan().context().spanId()).isEqualTo(parent.context().spanId());
		}
		finally {
			parent.end();
		}
		assertThat(this.tracing.registry.getCurrentObservation()).isNull();
	}

	@Test
	void replacesStalePropagationPropertiesWithoutMutatingNativePublish() {
		Mqtt5Publish original = Mqtt5Publish.builder().topic("sensors/temperature")
				.userProperties().add("traceparent", "stale").add("traceparent", "also-stale")
				.add("sensor-id", "sensor-42").add("sensor-id", "sensor-43").applyUserProperties().build();
		this.handler.handleMessage(MessageBuilder.withPayload(original).build());
		SpanData producer = producerSpan();
		assertThat(this.published.get().getUserProperties().asList())
				.filteredOn(property -> property.getName().toString().equals("traceparent"))
				.singleElement().satisfies(property -> assertThat(property.getValue().toString())
						.startsWith("00-" + producer.getTraceId() + "-" + producer.getSpanId() + "-"));
		assertThat(this.published.get().getUserProperties().asList())
				.filteredOn(property -> property.getName().toString().equals("sensor-id"))
				.extracting(property -> property.getValue().toString()).containsExactly("sensor-42", "sensor-43");
		assertThat(original.getUserProperties().asList()).hasSize(4);
		assertThat(original.getUserProperties().asList().get(0).getValue().toString()).isEqualTo("stale");
	}

	@Test
	void asyncSpanEndsOnlyWhenPublishCompletes() {
		this.handler.setAsync(true);
		this.handler.handleMessage(message());
		assertThat(this.tracing.spans).noneMatch(span -> span.getKind() == SpanKind.PRODUCER);
		assertThat(this.tracing.registry.getCurrentObservation()).isNull();
		this.publishFuture.complete(mock(Mqtt5PublishResult.class));
		assertThat(producerSpan().getStatus().getStatusCode()).isNotEqualTo(StatusCode.ERROR);
	}

	@Test
	void asyncFailureRecordsErrorAndEndsSpan() {
		this.handler.setAsync(true);
		this.handler.handleMessage(message());
		this.publishFuture.completeExceptionally(new IllegalStateException("broker unavailable"));
		assertThat(producerSpan().getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
	}

	@Test
	void cancelledPublishEndsSpanWithError() {
		this.handler.setAsync(true);
		this.handler.handleMessage(message());
		this.publishFuture.cancel(false);
		assertThat(producerSpan().getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void immediatePublishFailureRecordsErrorAndRestoresScope(boolean async) {
		this.handler.setAsync(async);
		IllegalStateException failure = new IllegalStateException("client disconnected");
		if (async) {
			when(this.asyncClient.publish(any(Mqtt5Publish.class))).thenThrow(failure);
		}
		else {
			when(this.blockingClient.publish(any(Mqtt5Publish.class))).thenThrow(failure);
		}
		assertThatThrownBy(() -> this.handler.handleMessage(message())).hasCause(failure);
		assertThat(producerSpan().getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
		assertThat(this.tracing.registry.getCurrentObservation()).isNull();
		assertThat(this.tracing.tracer.currentSpan()).isNull();
	}

	@Test
	void disabledObservationLeavesNativePublishUnchanged() {
		this.handler.registerObservationRegistry(ObservationRegistry.NOOP);
		this.handler.setAsync(true);
		Mqtt5Publish original = Mqtt5Publish.builder().topic("sensors/temperature").build();
		this.handler.handleMessage(MessageBuilder.withPayload(original).build());
		this.publishFuture.complete(mock(Mqtt5PublishResult.class));
		assertThat(this.published.get()).isSameAs(original);
		assertThat(this.tracing.spans).isEmpty();
	}

	private SpanData producerSpan() {
		List<SpanData> producers = this.tracing.spans.stream().filter(span -> span.getKind() == SpanKind.PRODUCER).toList();
		assertThat(producers).hasSize(1);
		return producers.get(0);
	}

	private static Message<?> message() {
		return MessageBuilder.withPayload("temperature=21")
				.setHeader(MqttHeaders.TOPIC, "sensors/temperature").build();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

	}

}
