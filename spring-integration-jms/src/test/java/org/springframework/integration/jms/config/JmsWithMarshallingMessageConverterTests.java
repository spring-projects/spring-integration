/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.config;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class JmsWithMarshallingMessageConverterTests extends ActiveMQMultiContextTests {

	@Autowired
	@Qualifier("outbound-gateway-channel")
	MessageChannel input;

	@Autowired
	PollableChannel output;

	@Test
	public void demoWithMarshallingConverter() {
		this.input.send(new GenericMessage<>("hello"));
		Message<?> replyMessage = this.output.receive();
		MessageHeaders headers = replyMessage.getHeaders();
		// check for couple of JMS headers, make sure they are present
		assertThat(headers.get("jms_redelivered")).isNotNull();
		assertThat(replyMessage.getPayload()).isEqualTo("HELLO");
	}

	public static class SampleService {

		public String echo(String value) {
			return value.toUpperCase();
		}

	}

	public static class SampleMarshaller implements Marshaller {

		public void marshal(Object graph, Result result) throws IOException, XmlMappingException {
			String payload;
			if (graph instanceof Message<?>) {
				payload = (String) ((Message<?>) graph).getPayload();
			}
			else {
				payload = (String) graph;
			}
			((StreamResult) result).getOutputStream().write(payload.getBytes());
		}

		public boolean supports(Class<?> clazz) {
			return true;
		}

	}

	public static class SampleUnmarshaller implements Unmarshaller {

		public boolean supports(Class<?> clazz) {
			return true;
		}

		public Object unmarshal(Source source) throws IOException, XmlMappingException {
			InputStream io = ((StreamSource) source).getInputStream();
			byte[] bytes = new byte[io.available()];
			io.read(bytes);
			return new GenericMessage<>(new String(bytes));
		}

	}

}
