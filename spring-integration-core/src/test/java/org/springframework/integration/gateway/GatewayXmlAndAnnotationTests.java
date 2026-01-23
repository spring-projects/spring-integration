/*
 * Copyright 2002-present the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class GatewayXmlAndAnnotationTests {

	@Autowired
	GatewayProxyFactoryBean<?> gatewayProxyFactoryBean;

	@Autowired
	AGateway<String> stringAGateway;

	@Test
	public void test() {
		assertThat(TestUtils.<Expression>getPropertyValue(gatewayProxyFactoryBean, "defaultReplyTimeout")
				.getValue()).isEqualTo(123L);
		Map<Method, MessagingGatewaySupport> gatewayMap =
				TestUtils.getPropertyValue(gatewayProxyFactoryBean, "gatewayMap");
		int assertions = 0;
		for (Entry<Method, MessagingGatewaySupport> entry : gatewayMap.entrySet()) {
			switch (entry.getKey().getName()) {
				case "annotationShouldNotOverrideDefault" -> {
					assertThat(TestUtils.<Long>getPropertyValue(entry.getValue(),
							"messagingTemplate.receiveTimeout")).isEqualTo(123L);
					assertions++;
				}
				case "annotationShouldOverrideDefault" -> {
					assertThat(TestUtils.<Long>getPropertyValue(entry.getValue(),
							"messagingTemplate.receiveTimeout")).isEqualTo(234L);
					assertions++;
				}
				case "annotationShouldOverrideDefaultToInfinity" -> {
					assertThat(TestUtils.<Long>getPropertyValue(entry.getValue(),
							"messagingTemplate.receiveTimeout")).isEqualTo(-1L);
					assertions++;
				}
				case "explicitTimeoutShouldOverrideDefault" -> {
					assertThat(TestUtils.<Long>getPropertyValue(entry.getValue(),
							"messagingTemplate.receiveTimeout")).isEqualTo(456L);
					assertions++;
				}
			}
		}
		assertThat(assertions).isEqualTo(4);
	}

	public interface AGateway<T> {

		@Gateway
		String annotationShouldNotOverrideDefault(T foo);

		@Gateway(replyTimeout = 234)
		String annotationShouldOverrideDefault(T foo);

		@Gateway(replyTimeout = -1)
		String annotationShouldOverrideDefaultToInfinity(T foo);

		String explicitTimeoutShouldOverrideDefault(T foo);

	}

}
