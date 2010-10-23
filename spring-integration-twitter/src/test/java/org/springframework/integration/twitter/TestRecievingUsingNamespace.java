/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.twitter;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;


/**
 * provides a simple example of a command-line client to receive DMs, messages, timeline updates.
 *
 * @author Josh Long
 */
@ContextConfiguration
public class TestRecievingUsingNamespace extends AbstractJUnit4SpringContextTests {

	@Autowired	private TwitterAnnouncer twitterAnnouncer;

	@Test
	//@Ignore
	public void testIt() throws Throwable {
		long ctr = 0;
		long s = 1000;

		while (ctr < (s * 60 * 3)) {
			Thread.sleep(s);
			ctr += s;
		}
	}
}
