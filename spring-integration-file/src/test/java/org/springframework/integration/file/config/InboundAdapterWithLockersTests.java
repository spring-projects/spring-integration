/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.file.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.file.locking.NioFileLocker;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class InboundAdapterWithLockersTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testAdaptersWithLockers() {
		assertThat(TestUtils.<Object>getPropertyValue(context.getBean("inputWithLockerA"),
				"source.scanner.locker")).isEqualTo(context.getBean("locker"));
		assertThat(TestUtils.<Object>getPropertyValue(context.getBean("inputWithLockerB"),
				"source.scanner.locker")).isEqualTo(context.getBean("locker"));
		assertThat(TestUtils.<Object>getPropertyValue(context.getBean("inputWithLockerC"),
				"source.scanner.locker")).isInstanceOf(NioFileLocker.class);
		assertThat(TestUtils.<Object>getPropertyValue(context.getBean("inputWithLockerD"),
				"source.scanner.locker")).isInstanceOf(NioFileLocker.class);
	}

}
