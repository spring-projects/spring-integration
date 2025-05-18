/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.integration.groovy.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 * @since 2.2
 */
public class ServiceActivatorParserTests {

	@Test
	public void failExpressionAndScript() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail-expression-and-script-context.xml",
					this.getClass()).close();
			fail("Expected exception");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage()
					.startsWith("Configuration problem: Neither 'ref' nor 'expression' are permitted when " +
							"an inner script element is configured on element 'service-activator' with id='test'."))
					.isTrue();
		}
	}

}
