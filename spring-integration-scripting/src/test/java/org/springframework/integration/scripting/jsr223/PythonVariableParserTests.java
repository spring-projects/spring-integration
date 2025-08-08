/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
