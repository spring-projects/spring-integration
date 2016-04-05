/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.scripting.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David Turanski
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DeriveLanguageFromExtensionTests {
	@Autowired
	private ApplicationContext ctx;

	@Test
	public void testParseLanguage() {
		String[] langs = { "ruby", "Groovy", "ECMAScript", "python" };
		Class<?>[] executors = {
				RubyScriptExecutor.class,
				DefaultScriptExecutor.class,
				DefaultScriptExecutor.class,
				PythonScriptExecutor.class
		};

		Map<String, ScriptExecutingMessageProcessor> scriptProcessors = ctx
				.getBeansOfType(ScriptExecutingMessageProcessor.class);
		assertEquals(4, scriptProcessors.size());

		for (int i = 0; i < 4; i++) {

			ScriptExecutingMessageProcessor processor = ctx.getBean(
					"org.springframework.integration.scripting.jsr223.ScriptExecutingMessageProcessor#" + i,
					ScriptExecutingMessageProcessor.class);

			AbstractScriptExecutor executor = (AbstractScriptExecutor) TestUtils.getPropertyValue(processor,
					"scriptExecutor");
			assertEquals(langs[i], executor.language);
			assertEquals(executors[i], executor.getClass());
		}
	}

	@Test
	public void testBadExtension() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail1-context.xml", this.getClass())
					.close();
		}
		catch (Exception e) {
			assertTrue(e.getMessage().contains("No suitable scripting engine found for extension 'xx'"));
		}
	}

	@Test
	public void testNoExtension() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail2-context.xml", this.getClass())
					.close();
		}
		catch (Exception e) {
			assertTrue(e.getMessage().contains("Unable to determine language for script 'foo'"));
		}
	}

}
