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

package org.springframework.integration.xmpp.ignore;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * this class demonstrates that when I launch this and then manipulate the status of the
 * user using Pidgin, the updated state is immediately delivered to the Spring Integration bus.
 *
 * @author Josh Long
 * @since 2.0
 */
@SpringJUnitConfig
@Disabled
public class InboundPresenceTests {

	@Test
	public void run() throws Exception {
		Thread.sleep(60 * 1000 * 1000);
	}

}
