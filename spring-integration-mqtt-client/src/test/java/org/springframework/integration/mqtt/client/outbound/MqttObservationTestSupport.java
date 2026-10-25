/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.outbound;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;

/**
 * @author Hyunah Oh
 *
 * @since 7.2
 */
final class MqttObservationTestSupport implements AutoCloseable {

	final List<SpanData> spans = new CopyOnWriteArrayList<>();

	final ObservationRegistry registry = ObservationRegistry.create();

	final OtelTracer tracer;

	private final OpenTelemetrySdk telemetry;

	MqttObservationTestSupport() {
		SpanProcessor processor = new SpanProcessor() {
			@Override
			public void onStart(Context context, ReadWriteSpan span) {
			}

			@Override
			public boolean isStartRequired() {
				return false;
			}

			@Override
			public void onEnd(ReadableSpan span) {
				MqttObservationTestSupport.this.spans.add(span.toSpanData());
			}

			@Override
			public boolean isEndRequired() {
				return true;
			}
		};
		ContextPropagators propagators = ContextPropagators.create(W3CTraceContextPropagator.getInstance());
		this.telemetry = OpenTelemetrySdk.builder()
				.setTracerProvider(SdkTracerProvider.builder().addSpanProcessor(processor).build())
				.setPropagators(propagators).build();
		var otelTracer = this.telemetry.getTracer("mqtt-observation-test");
		this.tracer = new OtelTracer(otelTracer, new OtelCurrentTraceContext(), event -> { });
		var propagator = new OtelPropagator(propagators, otelTracer);
		this.registry.observationConfig().observationHandler(
				new ObservationHandler.FirstMatchingCompositeObservationHandler(
						new PropagatingSenderTracingObservationHandler<>(this.tracer, propagator),
						new PropagatingReceiverTracingObservationHandler<>(this.tracer, propagator),
						new DefaultTracingObservationHandler(this.tracer)));
	}

	@Override
	public void close() {
		this.telemetry.close();
	}

}
