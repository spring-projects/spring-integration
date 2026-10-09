/*
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.transformer;

import org.springframework.integration.transformer.support.ProtoHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.ProtobufMessageConverter;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 * A Protocol Buffer transformer for generated {@link com.google.protobuf.Message} objects.
 * <p>
 * If the content type is set to {@code application/x-protobuf} (default if no content type),
 * then the output message payload is of type byte array.
 * <p>
 * If the content type is set to {@code application/json} and
 * the {@code com.google.protobuf:protobuf-java-util} dependency is on the
 * classpath, the output message payload of type String.
 *
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.1
 */
public class ToProtobufTransformer extends AbstractTransformer {

	private final ProtobufMessageConverter protobufMessageConverter;

	public ToProtobufTransformer() {
		this(new ProtobufMessageConverter());
	}

	public ToProtobufTransformer(ProtobufMessageConverter protobufMessageConverter) {
		Assert.notNull(protobufMessageConverter, "'protobufMessageConverter' must not be null");
		this.protobufMessageConverter = protobufMessageConverter;
	}

	@Override
	public String getComponentType() {
		return "to-protobuf-transformer";
	}

	@Override
	protected Object doTransform(Message<?> message) {
		Assert.isInstanceOf(com.google.protobuf.Message.class, message.getPayload(),
				"Payload must be an implementation of 'com.google.protobuf.Message'");

		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);
		accessor.setHeader(ProtoHeaders.TYPE, message.getPayload().getClass().getName());
		if (!message.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE)) {
			accessor.setContentType(ProtobufMessageConverter.PROTOBUF);
		}

		Message<?> result = this.protobufMessageConverter.toMessage(message.getPayload(), accessor.getMessageHeaders());
		if (result == null) {
			throw new MessageTransformationException(message, "Failed to convert to Protobuf");
		}
		return result;
	}

}
