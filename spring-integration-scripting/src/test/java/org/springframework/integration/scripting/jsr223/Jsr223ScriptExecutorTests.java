/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.scripting.jsr223;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.integration.scripting.jsr223.DefaultScriptExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scripting.support.StaticScriptSource;

/**
 * @author David Turanski
 *
 */
public class Jsr223ScriptExecutorTests {
	@Test
	public void test(){
		ScriptExecutor executor = new DefaultScriptExecutor("jruby");
		executor.executeScript(new StaticScriptSource("puts 'hello, world'"));
		executor.executeScript(new StaticScriptSource("puts 'hello, again'"));
		
		Map<String,Object> variables = new HashMap<String,Object>();
		
		Map<String,Object> headers = new HashMap<String,Object>();
		headers.put("one",1);
		headers.put("two","two");
		headers.put("three", new Integer(3));
		
		variables.put("payload", "payload");
		variables.put("headers", headers);
		 
		String result = (String)executor.executeScript(
				new ResourceScriptSource(new ClassPathResource("/org/springframework/integration/scripting/jsr223/print_message.rb")),
				variables
		);
		
		assertEquals("payload modified",result.substring(0,"payload modified".length()));
	}
	@Test
	public void testJs(){
		ScriptExecutor executor = new DefaultScriptExecutor("js");
		Object obj = executor.executeScript(new StaticScriptSource("function js(){ return 'js';} js();"));
		assertEquals("js",obj.toString());
	}
}
