/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.integration.config;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Marius Bogoevici
 * @author Gary Russell
 */
public class CorrelationStrategyInvalidConfigurationTests {

    @Test
    public void testCorrelationStrategyWithVoidReturningMethods() throws Exception {
		try {
			new ClassPathXmlApplicationContext("correlationStrategyWithVoidMethods.xml",
				CorrelationStrategyInvalidConfigurationTests.class).close();
		}
		catch (BeanCreationException e) {
			assertThat(e.getMessage(), containsString("MessageCountReleaseStrategy] has no eligible methods"));
		}
    }

    public static class VoidReturningCorrelationStrategy {

        public void invalidCorrelationMethod(String string) {
            //do nothing
        }

    }

}
