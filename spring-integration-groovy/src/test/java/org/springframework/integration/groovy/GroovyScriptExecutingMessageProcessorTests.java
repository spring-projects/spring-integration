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
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.core.io.AbstractResource;
import org.springframework.integration.Message;
import org.springframework.integration.groovy.config.RefreshableResourceScriptSource;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.test.annotation.Repeat;

/**
 * @author Mark Fisher
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class GroovyScriptExecutingMessageProcessorTests {

	@Rule
	public RepeatProcessor repeater = new RepeatProcessor(4);

	private AtomicInteger countHolder = new AtomicInteger();

	@Test
	@Repeat(20)
	public void testSimpleExecution() throws Exception {
		int count = countHolder.getAndIncrement();
		String script = "return \"payload is $payload, header is $headers.testHeader\"";
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("testHeader", "bar"+count).build();
		TestResource resource = new TestResource(script, "simpleTest");
		ScriptSource scriptSource = new ResourceScriptSource(resource);
		MessageProcessor<Object> processor = new GroovyScriptExecutingMessageProcessor(scriptSource, new DefaultScriptVariableSource());
		Object result = processor.processMessage(message);
		assertEquals("payload is foo, header is bar"+count, result.toString());
	}
	
	@Test
	public void testSimpleExecutionWithScriptVariableSource() throws Exception {
		int count = countHolder.getAndIncrement();
		String script = "return \"payload is $payload, header is $headers.testHeader and date is $date\"";
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("testHeader", "bar"+count).build();
		TestResource resource = new TestResource(script, "simpleTest");
		ScriptSource scriptSource = new ResourceScriptSource(resource);
		Object result = null;
		class CustomScriptVariableSource implements ScriptVariablesGenerator {
			public Map<String, Object> generateScriptVariables(Message<?> message) {
				Map<String, Object> variables = new HashMap<String, Object>();
				variables.put("date", System.nanoTime());
				variables.put("payload", message.getPayload());
				variables.put("headers", message.getHeaders());
				return variables;
			}
		}
		for (int i = 0; i < 5; i++) {
			ScriptVariablesGenerator scriptVariableSource = new CustomScriptVariableSource();		
			MessageProcessor<Object> processor = new GroovyScriptExecutingMessageProcessor(scriptSource, scriptVariableSource);
			Object newResult = processor.processMessage(message);
			assertFalse(newResult.equals(result)); // make sure that we get different nanotime verifying that resolveScriptVariables() is invoked
			result = newResult;
		}
	}

	@Test
	public void testLastModified() throws Exception {
		String script = "return \"payload is $payload, header is $headers.testHeader\"";
		TestResource resource = new TestResource(script, "simpleTest");
		long lastModified = resource.lastModified();
		Thread.sleep(20L);
		resource.setScript("foo");
		assertFalse("Expected last modified to change: "+lastModified+"=="+resource.lastModified(), lastModified==resource.lastModified());
	}

	@Test
	public void testModifiedScriptExecution() throws Exception {
		String script = "return \"payload is $payload, header is $headers.testHeader\"";
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("testHeader", "bar").build();
		TestResource resource = new TestResource(script, "simpleTest");
		ScriptSource scriptSource = new ResourceScriptSource(resource);
		MessageProcessor<Object> processor = new GroovyScriptExecutingMessageProcessor(scriptSource, new DefaultScriptVariableSource());
		Thread.sleep(20L);
		resource.setScript("return \"payload is $payload\"");
		Object result = processor.processMessage(message);
		assertEquals("payload is foo", result.toString());
	}

	@Test
	public void testRefreshableScriptExecution() throws Exception {
		String script = "return \"payload is $payload, header is $headers.testHeader\"";
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("testHeader", "bar").build();
		TestResource resource = new TestResource(script, "simpleTest");
		ScriptSource scriptSource = new RefreshableResourceScriptSource(resource, 1000L);
		MessageProcessor<Object> processor = new GroovyScriptExecutingMessageProcessor(scriptSource, new DefaultScriptVariableSource());
		// should be the original script
		Object result = processor.processMessage(message);
		assertEquals("payload is foo, header is bar", result.toString());
		//reset the script with the new strimg
		resource.setScript("return \"payload is $payload\"");
		Thread.sleep(20L);
		// should still assert to the old script because not enough time elapsed for refresh to kick in
		result = processor.processMessage(message);
		assertEquals("payload is foo, header is bar", result.toString());
		// sleep some more, past refresh time
		Thread.sleep(1000L);
		// now you go the new script
		resource.setScript("return \"payload is $payload\"");
		result = processor.processMessage(message);
		assertEquals("payload is foo", result.toString());
	}

	@Test
	public void testRefreshableScriptExecutionWithInfiniteDelay() throws Exception {
		String script = "return \"payload is $payload, header is $headers.testHeader\"";
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("testHeader", "bar").build();
		TestResource resource = new TestResource(script, "simpleTest");
		ScriptSource scriptSource = new RefreshableResourceScriptSource(resource, -1L);
		MessageProcessor<Object> processor = new GroovyScriptExecutingMessageProcessor(scriptSource, new DefaultScriptVariableSource());
		// process with the first script
		Object result = processor.processMessage(message);
		assertEquals("payload is foo, header is bar", result.toString());
		// change script, but since refresh-delay is less then 0 we should still se old script executing
		resource.setScript("return \"payload is $payload\"");
		// process and see assert that the old script is used
		result = processor.processMessage(message);
		assertEquals("payload is foo, header is bar", result.toString());
	}
	
	@Test
	public void testRefreshableScriptExecutionWithAlwaysRefresh() throws Exception {
		String script = "return \"payload is $payload, header is $headers.testHeader\"";
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("testHeader", "bar").build();
		TestResource resource = new TestResource(script, "simpleTest");
		ScriptSource scriptSource = new RefreshableResourceScriptSource(resource, 0);
		MessageProcessor<Object> processor = new GroovyScriptExecutingMessageProcessor(scriptSource, new DefaultScriptVariableSource());
		// process with the first script
		Object result = processor.processMessage(message);
		assertEquals("payload is foo, header is bar", result.toString());
		// change script, but since refresh-delay is less then 0 we should still se old script executing
		resource.setScript("return \"payload is $payload\"");
		// process and see assert that the old script is used
		result = processor.processMessage(message);
		assertEquals("payload is foo", result.toString());
		resource.setScript("return \"payload is 'hello'\"");
		// process and see assert that the old script is used
		result = processor.processMessage(message);
		assertEquals("payload is 'hello'", result.toString());
	}


	private static class TestResource extends AbstractResource {

		private String script;

		private final String filename;

		private long lastModified;

		private TestResource(String script, String filename) {
			setScript(script);
			this.filename = filename;
		}
		
		public long lastModified() throws IOException {
			return lastModified;
		}
		
		public void setScript(String script) {
			this.lastModified = System.currentTimeMillis();
			this.script = script;
		}
		
		public String getDescription() {
			return "test";
		}

		@Override
		public String getFilename() {
			return this.filename;
		}

		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(script.getBytes("UTF-8")); 
		}
	} 

}
