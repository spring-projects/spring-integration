/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.ws.config;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.ws.WebServiceHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class WebServiceHeaderEnricherTests {

	@Autowired @Qualifier("literalValueInput")
	private MessageChannel literalValueInput;

	@Autowired @Qualifier("expressionInput")
	private MessageChannel expressionInput;

	@Test
	public void literalValue() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(literalValueInput);
		Message<?> result = template.sendAndReceive(new GenericMessage<String>("foo"));
		Map<String, Object> headers = result.getHeaders();
		assertEquals("http://test", headers.get(WebServiceHeaders.SOAP_ACTION));
	}

	@Test
	public void expression() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(expressionInput);
		Message<?> result = template.sendAndReceive(new GenericMessage<String>("foo"));
		Map<String, Object> headers = result.getHeaders();
		assertEquals("http://foo", headers.get(WebServiceHeaders.SOAP_ACTION));
	}

}
