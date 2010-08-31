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

package org.springframework.integration.groovy;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import org.springframework.core.io.AbstractResource;
import org.springframework.integration.Message;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class GroovyScriptExecutingMessageProcessorTests {

	@Test
	public void simpleTest() throws Exception {
		String script = "return \"payload is $payload, header is $headers.testHeader\"";
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("testHeader", "bar").build();
		TestResource resource = new TestResource(script, "simpleTest");
		ScriptSource scriptSource = new ResourceScriptSource(resource);
		MessageProcessor processor = new GroovyScriptExecutingMessageProcessor(scriptSource);
		Object result = processor.processMessage(message);
		assertEquals("payload is foo, header is bar", result.toString());
	}


	private static class TestResource extends AbstractResource {

		private final String scriptString;

		private final String filename;

		private TestResource(String scriptString, String filename) {
			this.scriptString = scriptString;
			this.filename = filename;
		}
		
		public String getDescription() {
			return "test";
		}

		@Override
		public String getFilename() {
			return this.filename;
		}

		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(scriptString.getBytes("UTF-8")); 
		}
	} 

}
