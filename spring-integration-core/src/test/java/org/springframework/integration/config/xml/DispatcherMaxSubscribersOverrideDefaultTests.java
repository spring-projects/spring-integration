/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class DispatcherMaxSubscribersOverrideDefaultTests extends DispatcherMaxSubscribersTests {

	@Autowired
	private SubscribableChannel oneSub;

	@Test
	public void test() {
		this.doTestUnicast(456, 456, 123, 456, 234);
		doTestMulticast(789, 2);
	}

	@Test
	public void testExceed() {
		oneSub.subscribe(message -> {
		});
		try {
			oneSub.subscribe(message -> {
			});
			fail("Expected Exception");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Maximum subscribers exceeded");
		}
	}

}
