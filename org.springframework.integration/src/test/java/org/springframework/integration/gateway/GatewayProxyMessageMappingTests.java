/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;
import org.springframework.integration.annotation.Payload;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class GatewayProxyMessageMappingTests {

	private final QueueChannel channel = new QueueChannel();

	private volatile TestGateway gateway = null;


	@Before
	public void initializeGateway() throws Exception {
		GatewayProxyFactoryBean factoryBean = new GatewayProxyFactoryBean();
		factoryBean.setServiceInterface(TestGateway.class);
		factoryBean.setDefaultRequestChannel(channel);
		factoryBean.afterPropertiesSet();
		this.gateway = (TestGateway) factoryBean.getObject();
	}


	@Test
	public void payloadAndHeaderMapWithoutAnnotations() throws Exception {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("k1", "v1");
		m.put("k2", "v2");
		gateway.payloadAndHeaderMapWithoutAnnotations("foo", m);
		Message<?> result = channel.receive(0);
		assertNotNull(result);
		assertEquals("foo", result.getPayload());
		assertEquals("v1", result.getHeaders().get("k1"));
		assertEquals("v2", result.getHeaders().get("k2"));
	}

	@Test
	public void payloadAndHeaderMapWithAnnotations() throws Exception {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("k1", "v1");
		m.put("k2", "v2");
		gateway.payloadAndHeaderMapWithAnnotations("foo", m);
		Message<?> result = channel.receive(0);
		assertNotNull(result);
		assertEquals("foo", result.getPayload());
		assertEquals("v1", result.getHeaders().get("k1"));
		assertEquals("v2", result.getHeaders().get("k2"));
	}

	@Test
	public void headerValuesAndPayloadWithAnnotations() throws Exception {
		gateway.headerValuesAndPayloadWithAnnotations("headerValue1", "payloadValue", "headerValue2");
		Message<?> result = channel.receive(0);
		assertNotNull(result);
		assertEquals("payloadValue", result.getPayload());
		assertEquals("headerValue1", result.getHeaders().get("k1"));
		assertEquals("headerValue2", result.getHeaders().get("k2"));
	}

	@Test
	public void mapOnly() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("k1", "v1");
		map.put("k2", "v2");
		gateway.mapOnly(map);
		Message<?> result = channel.receive(0);
		assertNotNull(result);
		assertEquals(map, result.getPayload());
		assertNull(result.getHeaders().get("k1"));
		assertNull(result.getHeaders().get("k2"));
	}

	@Test
	public void twoMapsAndOneAnnotatedWithPayload() {
		Map<String, Object> map1 = new HashMap<String, Object>();
		Map<String, Object> map2 = new HashMap<String, Object>();
		map1.put("k1", "v1");
		map2.put("k2", "v2");
		gateway.twoMapsAndOneAnnotatedWithPayload(map1, map2);
		Message<?> result = channel.receive(0);
		assertNotNull(result);
		assertEquals(map1, result.getPayload());
		assertEquals("v2", result.getHeaders().get("k2"));
		assertNull(result.getHeaders().get("k1"));
	}

	@Test(expected = MessagingException.class)
	public void twoMapsWithoutAnnotations() {
		Map<String, Object> map1 = new HashMap<String, Object>();
		Map<String, Object> map2 = new HashMap<String, Object>();
		map1.put("k1", "v1");
		map2.put("k2", "v2");
		gateway.twoMapsWithoutAnnotations(map1, map2);
	}

	@Test(expected = MessagingException.class)
	public void twoPayloads() throws Exception {
		gateway.twoPayloads("won't", "work");
	}

	@Test(expected = MessagingException.class)
	public void payloadAndHeaderAnnotationsOnSameParameter() throws Exception {
		gateway.payloadAndHeaderAnnotationsOnSameParameter("oops");
	}

	@Test(expected = MessagingException.class)
	public void payloadAndHeadersAnnotationsOnSameParameter() throws Exception {
		gateway.payloadAndHeadersAnnotationsOnSameParameter(new HashMap<String, Object>());
	}

	@Test(expected = MessagingException.class)
	public void payloadWithExpression() throws Exception {
		gateway.payloadWithExpression("test");
	}


	public static interface TestGateway {

		void payloadAndHeaderMapWithoutAnnotations(String s, Map<String, Object> map);

		void payloadAndHeaderMapWithAnnotations(@Payload String s, @Headers Map<String, Object> map);

		void headerValuesAndPayloadWithAnnotations(@Header("k1") String x, @Payload String s, @Header("k2") String y);

		void mapOnly(Map<String, Object> map);

		void twoMapsAndOneAnnotatedWithPayload(@Payload Map<String, Object> payload, Map<String, Object> headers);

		// invalid
		void twoMapsWithoutAnnotations(Map<String, Object> m1, Map<String, Object> m2);

		// invalid
		void twoPayloads(@Payload String s1, @Payload String s2);

		// invalid
		void payloadAndHeaderAnnotationsOnSameParameter(@Payload @Header("x") String s);

		// invalid
		void payloadAndHeadersAnnotationsOnSameParameter(@Payload @Headers Map<String, Object> map);

		// invalid
		void payloadWithExpression(@Payload("oops") String s);

	}

}
