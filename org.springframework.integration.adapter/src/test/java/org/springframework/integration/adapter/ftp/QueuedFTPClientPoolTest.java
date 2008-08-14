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
package org.springframework.integration.adapter.ftp;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Iwein Fuld
 * 
 */
public class QueuedFTPClientPoolTest {

	private QueuedFTPClientPool pool;

	private FTPClientFactory factoryMock = createMock(FTPClientFactory.class);

	private Object[] allMocks = new Object[] { factoryMock };

	@Before
	public void initializeSubject() throws Exception {
		this.pool = new QueuedFTPClientPool(5);
		pool.setFactory(factoryMock);
	}

	@Test
	public void get() throws Exception {
		FTPClient expectedClient = new FTPClient();
		expect(factoryMock.getClient()).andReturn(expectedClient);
		replay(allMocks);
		FTPClient client = pool.getClient();
		assertEquals(expectedClient, client);
		verify(allMocks);
	}

	@Test
	public void getMultipleGet() throws Exception {
		FTPClient[] expectedClients = new FTPClient[] { new FTPClient(), new FTPClient(), new FTPClient(),
				new FTPClient(), new FTPClient(), new FTPClient() };
		for (FTPClient client : expectedClients) {
			expect(factoryMock.getClient()).andReturn(client);
		}
		replay(allMocks);
		for (int i = 0; i < 6; i++) {
			assertSame(expectedClients[i], pool.getClient());
		}
		verify(allMocks);
	}

	@Test
	public void getMultipleGetReleaseGet() throws Exception {
		FTPClient[] expectedClients = new FTPClient[] { new FTPClient(), new FTPClient(), new FTPClient(),
				new FTPClient(), new FTPClient() };
		for (FTPClient client : expectedClients) {
			expect(factoryMock.getClient()).andReturn(client);
		}
		replay(allMocks);
		List<FTPClient> fromPool = new ArrayList<FTPClient>();
		for (int i = 0; i < 5; i++) {
			fromPool.add(pool.getClient());
		}
		for (FTPClient client2 : fromPool) {
			pool.releaseClient(client2);
		}
		for (int i = 0; i < 5; i++) {
			FTPClient client = pool.getClient();
			boolean removed = fromPool.remove(client);
			assertTrue("Failed on element " + i, removed);
		}
		verify(allMocks);
	}
}
