/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.file.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.file.locking.NioFileLocker;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * 
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class InboundAdapterWithLockersTests {
	@Autowired(required = true)
	private ApplicationContext context;

	@Test
	public void testAdaptersWithLockers() {
		assertEquals(context.getBean("locker"), 
				TestUtils.getPropertyValue(context.getBean("inputWithLockerA"), "source.scanner.locker"));
		assertEquals(context.getBean("locker"), 
				TestUtils.getPropertyValue(context.getBean("inputWithLockerB"), "source.scanner.locker"));
		assertTrue(TestUtils.getPropertyValue(context.getBean("inputWithLockerC"), "source.scanner.locker") 
					instanceof NioFileLocker);
		assertTrue(TestUtils.getPropertyValue(context.getBean("inputWithLockerD"), "source.scanner.locker") 
					instanceof NioFileLocker);
	}
}
