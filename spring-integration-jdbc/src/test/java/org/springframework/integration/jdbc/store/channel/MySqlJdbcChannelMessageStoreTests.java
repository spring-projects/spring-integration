/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.jdbc.store.channel;

import org.junit.Before;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Gunnar Hillert
 */
@ContextConfiguration
//@RunWith(SpringJUnit4ClassRunner.class)
public class MySqlJdbcChannelMessageStoreTests extends AbstractJdbcChannelMessageStoreTests {

	@Before
	@Override
	public void init() throws Exception {
		super.init();
	}

	//@Test
	@Override
	public void testGetNonExistentMessageFromGroup() throws Exception {
		super.testGetNonExistentMessageFromGroup();
	}

	//@Test
	@Override
	public void testAddAndGet() throws Exception {
		super.testAddAndGet();
	}

}
