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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.adapter.file.Backlog;
import org.springframework.integration.adapter.file.FileInfo;

/**
 * @author Marius Bogoevici
 * @author Iwein Fuld
 */
public class BacklogTests {

	private Backlog<FileInfo> backlog;


	@Before
	public void setUp() {
		backlog = new Backlog<FileInfo>();
	}


	@Test
	public void testInitialization() {
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertEquals(3, backlog.getBacklog().size());
		Assert.assertTrue(backlog.getBacklog().containsKey("a.txt"));
		Assert.assertTrue(backlog.getBacklog().containsKey("b.txt"));
		Assert.assertTrue(backlog.getBacklog().containsKey("c.txt"));
	}

	@Test
	public void testFullProcessingInOneStep() {
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		backlog.processSnapshot(remoteSnapshot);
		backlog.fileProcessed("a.txt");
		backlog.fileProcessed("b.txt");
		backlog.fileProcessed("c.txt");
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertTrue(backlog.getBacklog().isEmpty());
	}

	@Test
	public void testFullProcessingInTwoSteps() {
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		backlog.processSnapshot(remoteSnapshot);
		backlog.fileProcessed("a.txt");
		backlog.fileProcessed("b.txt");
		Assert.assertEquals(1, backlog.getBacklog().size());
		Assert.assertTrue(backlog.getBacklog().containsKey("c.txt"));
		backlog.processSnapshot(remoteSnapshot);		
		Assert.assertEquals(1, backlog.getBacklog().size());
		Assert.assertTrue(backlog.getBacklog().containsKey("c.txt"));
		backlog.fileProcessed("c.txt");
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertTrue(backlog.getBacklog().isEmpty());
	}

	@Test
	public void testOneFileChangedSize() {
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		backlog.processSnapshot(remoteSnapshot);
		backlog.fileProcessed("a.txt");
		backlog.fileProcessed("b.txt");
		Assert.assertEquals(1, backlog.getBacklog().size());
		Assert.assertTrue(backlog.getBacklog().containsKey("c.txt"));
		backlog.processSnapshot(remoteSnapshot);		
		Assert.assertEquals(1, backlog.getBacklog().size());
		Assert.assertTrue(backlog.getBacklog().containsKey("c.txt"));
		backlog.fileProcessed("c.txt");
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		backlog.processSnapshot(remoteSnapshot);
		remoteSnapshot.put("c.txt", new FileInfo("c.txt", 1001, 112));
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertEquals(1, backlog.getBacklog().size());
		Assert.assertTrue(backlog.getBacklog().containsKey("c.txt"));
	}

	@Test
	public void testOneFileChangedDate() {
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		backlog.processSnapshot(remoteSnapshot);
		backlog.fileProcessed("a.txt");
		backlog.fileProcessed("b.txt");
		Assert.assertEquals(1, backlog.getBacklog().size());
		Assert.assertTrue(backlog.getBacklog().containsKey("c.txt"));
		backlog.processSnapshot(remoteSnapshot);		
		Assert.assertEquals(1, backlog.getBacklog().size());
		Assert.assertTrue(backlog.getBacklog().containsKey("c.txt"));
		backlog.fileProcessed("c.txt");
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		backlog.processSnapshot(remoteSnapshot);
		remoteSnapshot.put("c.txt", new FileInfo("c.txt", 1011, 102));
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertEquals(1, backlog.getBacklog().size());
		Assert.assertTrue(backlog.getBacklog().containsKey("c.txt"));
	}

	@Test
	public void testOneFileAdded() {
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		backlog.processSnapshot(remoteSnapshot);
		backlog.fileProcessed("a.txt");
		backlog.fileProcessed("b.txt");
		backlog.fileProcessed("c.txt");
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		backlog.processSnapshot(remoteSnapshot);
		remoteSnapshot.put("d.txt", new FileInfo("d.txt", 1003, 103));
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertEquals(1, backlog.getBacklog().size());
		Assert.assertTrue(backlog.getBacklog().containsKey("d.txt"));
	}

	@Test
	public void testOneFileRemoved() {
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		backlog.processSnapshot(remoteSnapshot);
		backlog.fileProcessed("a.txt");
		backlog.fileProcessed("b.txt");
		backlog.fileProcessed("c.txt");
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		backlog.processSnapshot(remoteSnapshot);
		remoteSnapshot.remove("c.txt");
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertTrue(backlog.getBacklog().isEmpty());
	}	

	@Test
	public void testOneFileRemovedBeforeBeingProcessedInTheNextStep() {
		Assert.assertTrue(backlog.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertTrue(backlog.getBacklog().containsKey("c.txt"));
		remoteSnapshot.remove("c.txt");
		backlog.processSnapshot(remoteSnapshot);		
		Assert.assertEquals(2, backlog.getBacklog().size());
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertEquals(2, backlog.getBacklog().size());
	}


	private static Map<String, FileInfo> generateInitialSnapshot() {
		Map<String, FileInfo> remoteSnapshot = new HashMap<String, FileInfo>();
		remoteSnapshot.put("a.txt", new FileInfo("a.txt", 1000, 100));
		remoteSnapshot.put("b.txt", new FileInfo("b.txt", 1001, 101));
		remoteSnapshot.put("c.txt", new FileInfo("c.txt", 1002, 102));
		return remoteSnapshot;
	}

}
