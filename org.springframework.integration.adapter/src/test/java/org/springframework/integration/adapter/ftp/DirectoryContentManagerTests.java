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

/**
 * @author Marius Bogoevici
 */
public class DirectoryContentManagerTests {

	private DirectoryContentManager directoryContentManager;


	@Before
	public void setUp() {
		directoryContentManager = new DirectoryContentManager();
	}


	@Test
	public void testInitialization() {
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		directoryContentManager.processSnapshot(remoteSnapshot);
		Assert.assertEquals(3, directoryContentManager.getBacklog().size());
		Assert.assertTrue(directoryContentManager.getBacklog().containsKey("a.txt"));
		Assert.assertTrue(directoryContentManager.getBacklog().containsKey("b.txt"));
		Assert.assertTrue(directoryContentManager.getBacklog().containsKey("c.txt"));
	}

	@Test
	public void testFullProcessingInOneStep() {
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		directoryContentManager.processSnapshot(remoteSnapshot);
		directoryContentManager.fileProcessed("a.txt");
		directoryContentManager.fileProcessed("b.txt");
		directoryContentManager.fileProcessed("c.txt");
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		directoryContentManager.processSnapshot(remoteSnapshot);
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
	}

	@Test
	public void testFullProcessingInTwoSteps() {
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		directoryContentManager.processSnapshot(remoteSnapshot);
		directoryContentManager.fileProcessed("a.txt");
		directoryContentManager.fileProcessed("b.txt");
		Assert.assertEquals(1, directoryContentManager.getBacklog().size());
		Assert.assertTrue(directoryContentManager.getBacklog().containsKey("c.txt"));
		directoryContentManager.processSnapshot(remoteSnapshot);		
		Assert.assertEquals(1, directoryContentManager.getBacklog().size());
		Assert.assertTrue(directoryContentManager.getBacklog().containsKey("c.txt"));
		directoryContentManager.fileProcessed("c.txt");
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		directoryContentManager.processSnapshot(remoteSnapshot);
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
	}

	@Test
	public void testOneFileChangedSize() {
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		directoryContentManager.processSnapshot(remoteSnapshot);
		directoryContentManager.fileProcessed("a.txt");
		directoryContentManager.fileProcessed("b.txt");
		Assert.assertEquals(1, directoryContentManager.getBacklog().size());
		Assert.assertTrue(directoryContentManager.getBacklog().containsKey("c.txt"));
		directoryContentManager.processSnapshot(remoteSnapshot);		
		Assert.assertEquals(1, directoryContentManager.getBacklog().size());
		Assert.assertTrue(directoryContentManager.getBacklog().containsKey("c.txt"));
		directoryContentManager.fileProcessed("c.txt");
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		directoryContentManager.processSnapshot(remoteSnapshot);
		remoteSnapshot.put("c.txt", new FileInfo("c.txt", 1001, 112));
		directoryContentManager.processSnapshot(remoteSnapshot);
		Assert.assertEquals(1, directoryContentManager.getBacklog().size());
		Assert.assertTrue(directoryContentManager.getBacklog().containsKey("c.txt"));
	}

	@Test
	public void testOneFileChangedDate() {
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		directoryContentManager.processSnapshot(remoteSnapshot);
		directoryContentManager.fileProcessed("a.txt");
		directoryContentManager.fileProcessed("b.txt");
		Assert.assertEquals(1, directoryContentManager.getBacklog().size());
		Assert.assertTrue(directoryContentManager.getBacklog().containsKey("c.txt"));
		directoryContentManager.processSnapshot(remoteSnapshot);		
		Assert.assertEquals(1, directoryContentManager.getBacklog().size());
		Assert.assertTrue(directoryContentManager.getBacklog().containsKey("c.txt"));
		directoryContentManager.fileProcessed("c.txt");
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		directoryContentManager.processSnapshot(remoteSnapshot);
		remoteSnapshot.put("c.txt", new FileInfo("c.txt", 1011, 102));
		directoryContentManager.processSnapshot(remoteSnapshot);
		Assert.assertEquals(1, directoryContentManager.getBacklog().size());
		Assert.assertTrue(directoryContentManager.getBacklog().containsKey("c.txt"));
	}

	@Test
	public void testOneFileAdded() {
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		directoryContentManager.processSnapshot(remoteSnapshot);
		directoryContentManager.fileProcessed("a.txt");
		directoryContentManager.fileProcessed("b.txt");
		directoryContentManager.fileProcessed("c.txt");
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		directoryContentManager.processSnapshot(remoteSnapshot);
		remoteSnapshot.put("d.txt", new FileInfo("d.txt", 1003, 103));
		directoryContentManager.processSnapshot(remoteSnapshot);
		Assert.assertEquals(1, directoryContentManager.getBacklog().size());
		Assert.assertTrue(directoryContentManager.getBacklog().containsKey("d.txt"));
	}

	@Test
	public void testOneFileRemoved() {
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		directoryContentManager.processSnapshot(remoteSnapshot);
		directoryContentManager.fileProcessed("a.txt");
		directoryContentManager.fileProcessed("b.txt");
		directoryContentManager.fileProcessed("c.txt");
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		directoryContentManager.processSnapshot(remoteSnapshot);
		remoteSnapshot.remove("c.txt");
		directoryContentManager.processSnapshot(remoteSnapshot);
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
	}	

	@Test
	public void testOneFileRemovedBeforeBeingProcessedInTheNextStep() {
		Assert.assertTrue(directoryContentManager.getBacklog().isEmpty());
		Map<String, FileInfo> remoteSnapshot = generateInitialSnapshot();
		directoryContentManager.processSnapshot(remoteSnapshot);
		Assert.assertTrue(directoryContentManager.getBacklog().containsKey("c.txt"));
		remoteSnapshot.remove("c.txt");
		directoryContentManager.processSnapshot(remoteSnapshot);		
		Assert.assertEquals(2, directoryContentManager.getBacklog().size());
		directoryContentManager.processSnapshot(remoteSnapshot);
		Assert.assertEquals(2, directoryContentManager.getBacklog().size());
	}


	private static Map<String, FileInfo> generateInitialSnapshot() {
		Map<String, FileInfo> remoteSnapshot = new HashMap<String, FileInfo>();
		remoteSnapshot.put("a.txt", new FileInfo("a.txt", 1000, 100));
		remoteSnapshot.put("b.txt", new FileInfo("b.txt", 1001, 101));
		remoteSnapshot.put("c.txt", new FileInfo("c.txt", 1002, 102));
		return remoteSnapshot;
	}

}
