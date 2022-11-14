/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MBeanAutoDetectTests {

	private MBeanServer server;

	private ClassPathXmlApplicationContext context;

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testRouterMBeanExistsWhenDefinedFirst() throws Exception {
		context = new ClassPathXmlApplicationContext("MBeanAutoDetectFirstTests-context.xml", getClass());
		server = context.getBean(MBeanServer.class);
		// System . err.println(server.queryNames(new ObjectName("test.MBeanAutoDetectFirst:*"), null));
		Set<ObjectName> names = server.queryNames(
				new ObjectName("test.MBeanAutoDetectFirst:type=ExpressionEvaluatingRouter,*"), null);
		assertThat(names.size()).isEqualTo(1);
	}

	@Test
	public void testRouterMBeanExistsWhenDefinedSecond() throws Exception {
		context = new ClassPathXmlApplicationContext("MBeanAutoDetectSecondTests-context.xml", getClass());
		server = context.getBean(MBeanServer.class);
		// System . err.println(server.queryNames(new ObjectName("test.MBeanAutoDetectFirst:*"), null));
		Set<ObjectName> names = server.queryNames(
				new ObjectName("test.MBeanAutoDetectFirst:type=ExpressionEvaluatingRouter,*"), null);
		assertThat(names.size()).isEqualTo(1);
	}

}
