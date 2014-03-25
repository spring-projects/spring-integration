/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.expression;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 3.0.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class ForeignClassloaderTests {

	@Autowired
	private MessageChannel foo;

	@Autowired
	private PollableChannel bar;

	/**
	 * Sends a message on a thread that has a ClassLoader that doesn't have visibility to Foo.
	 * Fails without a custom TypeLocator in the evaluation context factory bean.
	 */
	@Test
	public void testThreadHasWrongClassLoader() {
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					foo.send(new GenericMessage<String>("foo"));
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

		});
		t.setContextClassLoader(new ClassLoader() {

			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				throw new ClassNotFoundException("Foo not found");
			}
		});
		t.start();
		Message<?> reply = bar.receive(10000);
		assertThat(reply.getPayload(), instanceOf(Foo.class));
	}

	public static class Foo {

	}

}
