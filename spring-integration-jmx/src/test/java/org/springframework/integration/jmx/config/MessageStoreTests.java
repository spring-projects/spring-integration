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

package org.springframework.integration.jmx.config;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class MessageStoreTests {

	@Autowired
	private MBeanServer server;

	@Test
	public void testHandlerMBeanRegistration() throws Exception {
		Set<ObjectName> names = server.queryNames(new ObjectName("test.MessageStore:type=SimpleMessageStore,*"), null);
		assertThat(names.size()).isEqualTo(1);
		ObjectName name = names.iterator().next();
		assertThat(server.getAttribute(name, "MessageCount")).isEqualTo(0L);
		assertThat(server.getAttribute(name, "MessageGroupCount")).isEqualTo(0);
		assertThat(server.getAttribute(name, "MessageCountForAllMessageGroups")).isEqualTo(0);
	}

}
