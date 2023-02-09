/*
 * Copyright 2023 the original author or authors.
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
 * classpath, the output message payload if of type String.
 *
 * @author Christian Tzolov
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
	protected Object doTransform(Message<?> message) {
		Assert.isInstanceOf(com.google.protobuf.Message.class, message.getPayload(),
				"Payload must be an implementation of 'com.google.protobuf.Message'");

		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);
		accessor.setHeader(ProtoHeaders.TYPE, message.getPayload().getClass().getName());
		if (!message.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE)) {
			accessor.setContentType(ProtobufMessageConverter.PROTOBUF);
		}

		return this.protobufMessageConverter.toMessage(message.getPayload(), accessor.getMessageHeaders());
	}

}
