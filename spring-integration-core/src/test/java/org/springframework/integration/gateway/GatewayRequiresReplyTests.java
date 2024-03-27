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

package org.springframework.integration.gateway;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GatewayRequiresReplyTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void replyReceived() {
		TestService gateway = this.applicationContext.getBean("gateway", TestService.class);
		String result = gateway.test("foo");
		assertThat(result).isEqualTo("bar");
	}

	@Test(expected = ReplyRequiredException.class)
	public void noReplyReceived() {
		TestService gateway = this.applicationContext.getBean("gateway", TestService.class);
		gateway.test("bad");
	}

	@Test
	public void timedOutGateway() {
		TestService gateway = this.applicationContext.getBean("timeoutGateway", TestService.class);
		String result = gateway.test("hello");
		assertThat(result).isNull();
	}

	public interface TestService {

		String test(String s);

	}

	public static class LongRunningService {

		public String echo(String value) throws Exception {
			Thread.sleep(5000);
			return value;
		}

	}

}
