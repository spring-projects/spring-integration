/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.amqp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 *
 */
public class AmqpOutboundGatewayParserTests {

	@Test
	public void testGatewayConfig(){
		ApplicationContext context = new ClassPathXmlApplicationContext("AmqpOutboundGatewayParserTests-context.xml", this.getClass());
		AmqpOutboundEndpoint gateway = context.getBean(AmqpOutboundEndpoint.class);
		assertEquals(5, gateway.getOrder());
		assertTrue(context.containsBean("rabbitGateway"));
		assertEquals(context.getBean("fromRabbit"), TestUtils.getPropertyValue(gateway, "outputChannel"));
	}
}
