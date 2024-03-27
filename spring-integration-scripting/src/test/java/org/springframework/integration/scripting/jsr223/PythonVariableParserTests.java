/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.scripting.jsr223;

import java.io.IOException;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 *
 */
public class PythonVariableParserTests {

	@Test
	public void testBasic() throws IOException {
		String var = PythonScriptExecutor.PythonVariableParser.parseReturnVariable("x=2");
		assertThat(var).isEqualTo("x");

		var = PythonScriptExecutor.PythonVariableParser.parseReturnVariable("\n\n\nx  =  2\n\n\n");
		assertThat(var).isEqualTo("x");

		var = PythonScriptExecutor.PythonVariableParser.parseReturnVariable("\n\n\nx\n\n\n");
	}

	@Test
	public void test2() throws IOException {
		ScriptSource source =
				new ResourceScriptSource(new ClassPathResource("/org/springframework/integration/scripting/jsr223/test2.py"));
		String var = PythonScriptExecutor.PythonVariableParser.parseReturnVariable(source.getScriptAsString());
		assertThat(var).isEqualTo("bar");
	}

}
