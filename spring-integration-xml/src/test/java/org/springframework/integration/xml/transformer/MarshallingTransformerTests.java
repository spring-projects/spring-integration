/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.transformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;

import org.junit.jupiter.api.Test;

import org.springframework.integration.xml.result.StringResultFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.xml.transform.StringResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MarshallingTransformerTests {

	@Test
	public void testStringToStringResult() {
		TestMarshaller marshaller = new TestMarshaller();
		MarshallingTransformer transformer = new MarshallingTransformer(marshaller);
		transformer.setResultFactory(new StringResultFactory());
		Message<?> resultMessage = transformer.transform(new GenericMessage<>("world"));
		Object resultPayload = resultMessage.getPayload();
		assertThat(resultPayload.getClass()).isEqualTo(StringResult.class);
		assertThat(resultPayload.toString()).isEqualTo("hello world");
		assertThat(marshaller.payloads.get(0)).isEqualTo("world");
	}

	@Test
	public void testDefaultResultFactory() {
		TestMarshaller marshaller = new TestMarshaller();
		MarshallingTransformer transformer = new MarshallingTransformer(marshaller);
		Message<?> resultMessage = transformer.transform(new GenericMessage<>("world"));
		Object resultPayload = resultMessage.getPayload();
		assertThat(resultPayload.getClass()).isEqualTo(DOMResult.class);
		assertThat(marshaller.payloads.get(0)).isEqualTo("world");
	}

	@Test
	public void testMarshallingEntireMessage() {
		TestMarshaller marshaller = new TestMarshaller();
		MarshallingTransformer transformer = new MarshallingTransformer(marshaller);
		transformer.setExtractPayload(false);
		Message<?> message = new GenericMessage<>("test");
		transformer.transform(message);
		assertThat(marshaller.payloads.size()).isEqualTo(0);
		assertThat(marshaller.messages.size()).isEqualTo(1);
		assertThat(marshaller.messages.get(0)).isSameAs(message);
	}

	private static class TestMarshaller implements Marshaller {

		private final List<Message<?>> messages = new ArrayList<>();

		private final List<Object> payloads = new ArrayList<>();

		TestMarshaller() {
			super();
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		public void marshal(Object source, Result result) throws XmlMappingException, IOException {
			if (source instanceof Message) {
				this.messages.add((Message<?>) source);
			}
			else {
				this.payloads.add(source);
			}
			if (result instanceof StringResult) {
				((StringResult) result).getWriter().write("hello " + source);
			}
		}

	}

}
