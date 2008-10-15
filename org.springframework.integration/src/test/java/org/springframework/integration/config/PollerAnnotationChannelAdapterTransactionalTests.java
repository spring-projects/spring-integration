/*
 * Copyright 2002-2008 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.util.TestTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionDefinition;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PollerAnnotationChannelAdapterTransactionalTests {

	@Autowired @Qualifier("output")
	private PollableChannel output;

	@Autowired
	private PollerAnnotationChannelAdapterTransactionalTestBean adapter;

	@Autowired
	private TestTransactionManager transactionManager;


	@Before
	public void resetTransactionManager() {
		transactionManager.reset();
	}


	@Test
	public void commit() throws InterruptedException {
		adapter.setShouldFail(false);
		adapter.setNextValue("commit-test");
		transactionManager.waitForCompletion(1000);
		Message<?> reply = output.receive(1000);
		assertEquals("commit-test", reply.getPayload());
		assertEquals(1, transactionManager.getCommitCount());
		assertEquals(0, transactionManager.getRollbackCount());
	}

	@Test
	public void rollback() throws InterruptedException {
		adapter.setShouldFail(true);
		adapter.setNextValue("rollback-test");
		transactionManager.waitForCompletion(1000);
		assertNull(output.receive(0));
		assertEquals(0, transactionManager.getCommitCount());
		assertEquals(1, transactionManager.getRollbackCount());
	}

	@Test
	public void verifyPropagationSetting() throws InterruptedException {
		adapter.setShouldFail(false);
		adapter.setNextValue("propagation-test");
		transactionManager.waitForCompletion(1000);
		Message<?> reply = output.receive(1000);
		assertEquals("propagation-test", reply.getPayload());
		assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW,
				transactionManager.getLastDefinition().getPropagationBehavior());
	}

}
