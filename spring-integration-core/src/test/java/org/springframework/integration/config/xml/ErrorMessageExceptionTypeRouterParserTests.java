/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.config.xml;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ErrorMessageExceptionTypeRouterParserTests {

	@Autowired
	private MessageChannel inputChannel;

	@Autowired
	private QueueChannel defaultChannel;

	@Autowired
	private QueueChannel illegalChannel;

	@Autowired
	private QueueChannel npeChannel;

	@Test
	public void validateExceptionTypeRouterConfig(){

		inputChannel.send(new ErrorMessage(new NullPointerException()));
		assertTrue(npeChannel.receive(1000).getPayload() instanceof NullPointerException);

		inputChannel.send(new ErrorMessage(new IllegalArgumentException()));
		assertTrue(illegalChannel.receive(1000).getPayload()  instanceof IllegalArgumentException);

		inputChannel.send(new ErrorMessage(new RuntimeException()));
		assertTrue(defaultChannel.receive(1000).getPayload()  instanceof RuntimeException);
	}
}
