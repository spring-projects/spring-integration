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

package org.springframework.integration.jmx.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @since 2.0
 */
public class RouterMBeanTests {

	private MBeanServer server;

	private ClassPathXmlApplicationContext context;

	public void initContext(String configLocation) {
		context = new ClassPathXmlApplicationContext(configLocation, getClass());
		server = context.getBean(MBeanServer.class);
	}

	public static List<String> contexts() {
		return Arrays.asList(
				"RouterMBeanTests-context.xml",
				"RouterMBeanGatewayTests-context.xml",
				"RouterMBeanNoneTests-context.xml",
				"RouterMBeanSwitchTests-context.xml");
	}

	@AfterEach
	public void close() {
		if (context != null) {
			context.close();
		}
	}

	@ParameterizedTest
	@MethodSource("contexts")
	public void testRouterMBeanExists(String context) throws Exception {
		initContext(context);
		// System . err.println(server.queryNames(new ObjectName("test.RouterMBean:*"), null));
		Set<ObjectName> names = server.queryNames(
				new ObjectName("test.RouterMBean:type=MessageHandler,name=ptRouter,*"), null);
		assertThat(names.size()).isEqualTo(1);
	}

	@ParameterizedTest
	@MethodSource("contexts")
	public void testInputChannelMBeanExists(String context) throws Exception {
		initContext(context);
		// System . err.println(server.queryNames(new ObjectName("test.RouterMBean:type=MessageChannel,*"), null));
		Set<ObjectName> names = server.queryNames(
				new ObjectName("test.RouterMBean:type=MessageChannel,name=testChannel,*"), null);
		assertThat(names.size()).isEqualTo(1);
	}

	@ParameterizedTest
	@MethodSource("contexts")
	public void testErrorChannelMBeanExists(String context) throws Exception {
		initContext(context);
		Set<ObjectName> names = server.queryNames(
				new ObjectName("test.RouterMBean:type=MessageChannel,name=errorChannel,*"), null);
		assertThat(names.size()).isEqualTo(1);
	}

	@ParameterizedTest
	@MethodSource("contexts")
	public void testRouterMBeanOnlyRegisteredOnce(String context) throws Exception {
		initContext(context);
		// System . err.println(server.queryNames(new ObjectName("*:type=MessageHandler,*"), null));
		Set<ObjectName> names = server.queryNames(new ObjectName("test.RouterMBean:type=MessageHandler,name=ptRouter,*"), null);
		assertThat(names.size()).isEqualTo(1);
		// INT-3896
		names = server.queryNames(new ObjectName("test.RouterMBean:type=ExpressionEvaluatingRouter,*"), null);
		assertThat(names.size()).isEqualTo(0);
	}

	public interface Service {

		void send(String input);

	}

}
