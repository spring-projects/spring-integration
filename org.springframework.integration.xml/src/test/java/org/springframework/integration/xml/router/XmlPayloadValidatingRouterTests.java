package org.springframework.integration.xml.router;

import static org.junit.Assert.*;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.junit.Before;
/*
 * Copyright 2002-2008 the original author or authors.
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

import org.junit.Test;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;

public class XmlPayloadValidatingRouterTests {

	String validChannelName = "VALID";

	String invalidChannelName = "INVALID";
	
	Source testSource;
	
	Message<Source> testMessage;
	
	@Before
	public void setUp(){
		testSource = new SAXSource();
		testMessage = MessageBuilder.withPayload(testSource).build();
	}

	@Test
	public void testValidMessage(){
		StubValidator validator = new StubValidator(true);
		XmlPayloadValidatingRouter router = new XmlPayloadValidatingRouter(validChannelName, invalidChannelName, validator);
		String returnedChannelName = router.determineTargetChannelName(testMessage);
		assertEquals("Wrong channel name", validChannelName, returnedChannelName);
		assertEquals("Source not passed to validator ", testSource, validator.passedIn);
	}
	
	@Test
	public void testInvalidMessage(){
		StubValidator validator = new StubValidator(false);
		XmlPayloadValidatingRouter router = new XmlPayloadValidatingRouter(validChannelName, invalidChannelName, validator);
		String returnedChannelName = router.determineTargetChannelName(testMessage);
		assertEquals("Wrong channel name", invalidChannelName, returnedChannelName);
		assertEquals("Source not passed to validator ", testSource, validator.passedIn);
	}
	

	static class StubValidator implements XmlValidator {

		private final boolean validationResult;
		
		Source passedIn;
		
		public StubValidator(boolean validationResult) {
			this.validationResult = validationResult;
		}
		
		public boolean isValid(Source source) {
			passedIn = source;
			return validationResult;
		}

	}

}
