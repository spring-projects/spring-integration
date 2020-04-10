/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.transformer;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.core.codec.Encoder;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.support.cloudevents.ContentTypeDelegatingDataMarshaller;
import org.springframework.integration.support.cloudevents.Marshallers;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import io.cloudevents.CloudEvent;
import io.cloudevents.extensions.ExtensionFormat;
import io.cloudevents.format.Wire;
import io.cloudevents.format.builder.EventStep;
import io.cloudevents.v1.AttributesImpl;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;

/**
 * An {@link AbstractTransformer} implementation to build a cloud event
 * from the request message.
 * <p>
 * This transformer may produce a message according a {@link ToCloudEventTransformer.Result} option.
 * By default it is a {@link ToCloudEventTransformer.Result#RAW}
 * with the meaning to produce a {@link io.cloudevents.CloudEvent}
 * instance as a reply message payload.
 * <p>
 * A {@link ToCloudEventTransformer.Result#BINARY} mode produces a marshalled into a {@code byte[]}
 * a built {@link io.cloudevents.CloudEvent} body and respective cloud event headers.
 * <p>
 * A {@link ToCloudEventTransformer.Result#STRUCTURED} mode produces a marshalled into a {@code byte[]}
 * a whole {@link io.cloudevents.CloudEvent} and respective content type header
 * with the {@code "application/cloudevents+json"} value.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class ToCloudEventTransformer extends AbstractTransformer {

	public enum Result {

		RAW, BINARY, STRUCTURED

	}

	private final URI source;

	private final ContentTypeDelegatingDataMarshaller dataMarshaller = new ContentTypeDelegatingDataMarshaller();

	@Nullable
	private final EventStep<AttributesImpl, Object, byte[], String> wireBuilder;

	private Expression typeExpression =
			new FunctionExpression<Message<?>>((message) -> message.getPayload().getClass().getName());

	@Nullable
	private Expression subjectExpression;

	@Nullable
	private Expression dataSchemaExpression;

	@Nullable
	private Expression extensionExpression;

	private EvaluationContext evaluationContext;

	public ToCloudEventTransformer(URI source) {
		this(source, Result.RAW);
	}

	public ToCloudEventTransformer(URI source, Result resultMode) {
		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(resultMode, "'resultMode' must not be null");
		this.source = source;
		switch (resultMode) {
			case BINARY:
				this.wireBuilder = Marshallers.binary(this.dataMarshaller);
				break;
			case STRUCTURED:
				this.wireBuilder = Marshallers.structured();
				break;
			default:
				this.wireBuilder = null;
		}
	}

	public void setTypeExpression(Expression typeExpression) {
		Assert.notNull(typeExpression, "'typeExpression' must not be null");
		this.typeExpression = typeExpression;
	}

	public void setSubjectExpression(@Nullable Expression subjectExpression) {
		this.subjectExpression = subjectExpression;
	}

	public void setDataSchemaExpression(@Nullable Expression dataSchemaExpression) {
		this.dataSchemaExpression = dataSchemaExpression;
	}

	public void setExtensionExpression(@Nullable Expression extensionExpression) {
		this.extensionExpression = extensionExpression;
	}

	/**
	 * Configure a set of {@link Encoder}s for content type based data marshalling.
	 * They are used only for the the {@link Result#BINARY} mode and when inbound payload
	 * is not a {@code byte[]} already.
	 * Plus {@link MessageHeaders#CONTENT_TYPE} must be present in the request message.
	 * @param encoders the {@link Encoder}s to use.
	 */
	public final void setEncoders(Encoder<?>... encoders) {
		this.dataMarshaller.setEncoders(encoders);
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	protected Object doTransform(Message<?> message) {
		CloudEventImpl<Object> cloudEvent = buildCloudEvent(message);

		if (this.wireBuilder != null) {
			Wire<byte[], String, String> wire =
					this.wireBuilder.withEvent(() -> cloudEvent)
							.marshal();

			return getMessageBuilderFactory()
					.withPayload(wire.getPayload().orElse(cloudEvent.getDataBase64()))
					.copyHeaders(wire.getHeaders())
					.copyHeadersIfAbsent(message.getHeaders())
					.build();
		}
		else {
			return cloudEvent;
		}
	}

	@SuppressWarnings("unchecked")
	private CloudEventImpl<Object> buildCloudEvent(Message<?> message) {
		MessageHeaders headers = message.getHeaders();
		Object payload = message.getPayload();

		CloudEventBuilder<Object> cloudEventBuilder =
				payload instanceof CloudEvent
						? CloudEventBuilder.builder((CloudEvent<AttributesImpl, Object>) payload)
						: CloudEventBuilder.builder();

		cloudEventBuilder.withId(headers.getId() != null
				? headers.getId().toString()
				: UUID.randomUUID().toString())
				.withTime(ZonedDateTime.now())
				.withSource(this.source)
				.withType(this.typeExpression.getValue(this.evaluationContext, message, String.class));

		if (!(payload instanceof CloudEvent)) {
			if (payload instanceof byte[]) {
				cloudEventBuilder.withDataBase64((byte[]) payload);
			}
			else {
				cloudEventBuilder.withData(payload);
			}
		}

		MimeType contentType = StaticMessageHeaderAccessor.getContentType(message);

		if (contentType != null) {
			cloudEventBuilder.withDataContentType(contentType.toString());
		}

		if (this.subjectExpression != null) {
			cloudEventBuilder.withSubject(
					this.subjectExpression.getValue(this.evaluationContext, message, String.class));
		}

		if (this.dataSchemaExpression != null) {
			cloudEventBuilder.withDataschema(
					this.dataSchemaExpression.getValue(this.evaluationContext, message, URI.class));
		}

		if (this.extensionExpression != null) {
			cloudEventBuilder.withExtension(
					this.extensionExpression.getValue(this.evaluationContext, message, ExtensionFormat.class));
		}

		return cloudEventBuilder.build();
	}

}
