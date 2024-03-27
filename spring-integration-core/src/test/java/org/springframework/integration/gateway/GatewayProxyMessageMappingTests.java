/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.gateway;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class GatewayProxyMessageMappingTests {

	private final QueueChannel channel = new QueueChannel();

	private volatile TestGateway gateway = null;

	@BeforeEach
	public void initializeGateway() {
		GatewayProxyFactoryBean<TestGateway> factoryBean = new GatewayProxyFactoryBean<>(TestGateway.class);
		factoryBean.setDefaultRequestChannel(channel);
		factoryBean.setBeanName("testGateway");
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				new RootBeanDefinition(IntegrationEvaluationContextFactoryBean.class));
		context.refresh();
		factoryBean.setBeanFactory(context);
		factoryBean.afterPropertiesSet();
		this.gateway = factoryBean.getObject();
	}

	@Test
	public void payloadAndHeaderMapWithoutAnnotations() {
		Map<String, Object> m = new HashMap<>();
		m.put("k1", "v1");
		m.put("k2", "v2");
		gateway.payloadAndHeaderMapWithoutAnnotations("foo", m);
		Message<?> result = channel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("foo");
		assertThat(result.getHeaders().get("k1")).isEqualTo("v1");
		assertThat(result.getHeaders().get("k2")).isEqualTo("v2");
	}

	@Test
	public void payloadAndHeaderMapWithAnnotations() {
		Map<String, Object> m = new HashMap<>();
		m.put("k1", "v1");
		m.put("k2", "v2");
		gateway.payloadAndHeaderMapWithAnnotations("foo", m);
		Message<?> result = channel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("foo");
		assertThat(result.getHeaders().get("k1")).isEqualTo("v1");
		assertThat(result.getHeaders().get("k2")).isEqualTo("v2");
	}

	@Test
	public void headerValuesAndPayloadWithAnnotations() {
		gateway.headerValuesAndPayloadWithAnnotations("headerValue1", "payloadValue", "headerValue2");
		Message<?> result = channel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("payloadValue");
		assertThat(result.getHeaders().get("k1")).isEqualTo("headerValue1");
		assertThat(result.getHeaders().get("k2")).isEqualTo("headerValue2");
	}

	@Test
	public void mapOnly() {
		Map<String, Object> map = new HashMap<>();
		map.put("k1", "v1");
		map.put("k2", "v2");
		gateway.mapOnly(map);
		Message<?> result = channel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(map);
		assertThat(result.getHeaders().get("k1")).isNull();
		assertThat(result.getHeaders().get("k2")).isNull();
	}

	@Test
	public void twoMapsAndOneAnnotatedWithPayload() {
		Map<String, Object> map1 = new HashMap<>();
		Map<String, Object> map2 = new HashMap<>();
		map1.put("k1", "v1");
		map2.put("k2", "v2");
		gateway.twoMapsAndOneAnnotatedWithPayload(map1, map2);
		Message<?> result = channel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(map1);
		assertThat(result.getHeaders().get("k2")).isEqualTo("v2");
		assertThat(result.getHeaders().get("k1")).isNull();
	}

	@Test
	public void payloadAnnotationAtMethodLevel() {
		gateway.payloadAnnotationAtMethodLevel("foo", "bar");
		Message<?> result = channel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("foobar!");
	}

	@Test
	public void payloadAnnotationAtMethodLevelUsingBeanResolver() {
		GenericApplicationContext context = new GenericApplicationContext();
		RootBeanDefinition gatewayDefinition = new RootBeanDefinition(GatewayProxyFactoryBean.class);
		gatewayDefinition.getPropertyValues().add("defaultRequestChannel", channel);
		gatewayDefinition.getConstructorArgumentValues().addGenericArgumentValue(TestGateway.class);
		context.registerBeanDefinition("testGateway", gatewayDefinition);
		context.registerBeanDefinition("testBean", new RootBeanDefinition(TestBean.class));
		context.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				new RootBeanDefinition(IntegrationEvaluationContextFactoryBean.class));
		context.refresh();
		TestGateway gateway = context.getBean("testGateway", TestGateway.class);
		gateway.payloadAnnotationAtMethodLevelUsingBeanResolver("foo");
		Message<?> result = channel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("FOO!!!");
		context.close();
	}

	@Test
	public void payloadAnnotationWithExpression() {
		gateway.payloadAnnotationWithExpression("foo");
		Message<?> result = channel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("FOO");
	}

	@Test
	public void payloadAnnotationWithExpressionUsingBeanResolver() {
		GenericApplicationContext context = new GenericApplicationContext();
		RootBeanDefinition gatewayDefinition = new RootBeanDefinition(GatewayProxyFactoryBean.class);
		gatewayDefinition.getPropertyValues().add("defaultRequestChannel", channel);
		gatewayDefinition.getConstructorArgumentValues().addGenericArgumentValue(TestGateway.class);
		context.registerBeanDefinition("testGateway", gatewayDefinition);
		context.registerBeanDefinition("testBean", new RootBeanDefinition(TestBean.class));
		context.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				new RootBeanDefinition(IntegrationEvaluationContextFactoryBean.class));
		context.refresh();
		TestGateway gateway = context.getBean("testGateway", TestGateway.class);
		gateway.payloadAnnotationWithExpressionUsingBeanResolver("foo");
		gateway.payloadAnnotationWithExpressionUsingBeanResolver("bar");
		Message<?> fooResult = channel.receive(0);
		assertThat(fooResult).isNotNull();
		assertThat(fooResult.getPayload()).isEqualTo(324);
		Message<?> barResult = channel.receive(0);
		assertThat(barResult).isNotNull();
		assertThat(barResult.getPayload()).isEqualTo(309);
		assertThat(channel.receive(0)).isNull();
		context.close();
	}

	@Test
	public void twoMapsWithoutAnnotations() {
		Map<String, Object> map1 = new HashMap<>();
		Map<String, Object> map2 = new HashMap<>();
		map1.put("k1", "v1");
		map2.put("k2", "v2");
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.gateway.twoMapsWithoutAnnotations(map1, map2));
	}

	@Test
	public void twoPayloads() {
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.gateway.twoPayloads("won't", "work"));
	}

	@Test
	public void payloadAndHeaderAnnotationsOnSameParameter() {
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.gateway.payloadAndHeaderAnnotationsOnSameParameter("oops"));
	}

	@Test
	public void payloadAndHeadersAnnotationsOnSameParameter() {
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.gateway.payloadAndHeadersAnnotationsOnSameParameter(new HashMap<>()));
	}

	public interface TestGateway {

		void payloadAndHeaderMapWithoutAnnotations(String s, Map<String, Object> map);

		void payloadAndHeaderMapWithAnnotations(@Payload String s, @Headers Map<String, Object> map);

		void headerValuesAndPayloadWithAnnotations(@Header("k1") String x, @Payload String s, @Header("k2") String y);

		void mapOnly(Map<String, Object> map);

		void twoMapsAndOneAnnotatedWithPayload(@Payload Map<String, Object> payload, Map<String, Object> headers);

		@Payload("args[0] + args[1] + '!'")
		void payloadAnnotationAtMethodLevel(String a, String b);

		@Payload("@testBean.exclaim(args[0])")
		void payloadAnnotationAtMethodLevelUsingBeanResolver(String s);

		void payloadAnnotationWithExpression(@Payload("toUpperCase()") String s);

		void payloadAnnotationWithExpressionUsingBeanResolver(@Payload("@testBean.sum(#this)") String s);

		// invalid
		void twoMapsWithoutAnnotations(Map<String, Object> m1, Map<String, Object> m2);

		// invalid
		void twoPayloads(@Payload String s1, @Payload String s2);

		// invalid
		void payloadAndHeaderAnnotationsOnSameParameter(@Payload @Header("x") String s);

		// invalid
		void payloadAndHeadersAnnotationsOnSameParameter(@Payload @Headers Map<String, Object> map);

	}

	public static class TestBean {

		public String exclaim(String s) {
			return s.toUpperCase() + "!!!";
		}

		public int sum(String s) {
			int sum = 0;
			for (byte b : s.getBytes()) {
				sum += b;
			}
			return sum;
		}

	}

}
