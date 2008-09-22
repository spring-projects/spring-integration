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

import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.adapter.ftp.Backlog;
import org.springframework.integration.adapter.ftp.FileSnapshot;

/**
 * @author Marius Bogoevici
 * @author Iwein Fuld
 */
@SuppressWarnings("unchecked")
public class BacklogTests {

	private Backlog<FileSnapshot> backlog;

	private ArrayList<FileSnapshot> remoteSnapshot;
	
	private FileSnapshot[] process;

	@Before
	public void setUp() {
		backlog = new Backlog<FileSnapshot>();
		process = new FileSnapshot[5];
	}

	@Test
	public void testInitialization() {
		Assert.assertTrue(backlog.isEmpty());
		backlog.processSnapshot(remoteSnapshot);
		PriorityBlockingQueue<FileSnapshot> queue = (PriorityBlockingQueue<FileSnapshot>) new DirectFieldAccessor(
				backlog).getPropertyValue("backlog");
		Assert.assertEquals(3, queue.size());
		Assert.assertTrue(queue.containsAll(remoteSnapshot));
	}

	@Test
	public void testFullProcessingInOneStep() {
		backlog.processSnapshot(remoteSnapshot);
		backlog.fileProcessed( remoteSnapshot.toArray(process));
		Assert.assertTrue(backlog.isEmpty());
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertTrue(backlog.isEmpty());
	}

	@Test
	public void testFullProcessingInTwoSteps() {
		backlog.processSnapshot(remoteSnapshot);
		backlog.fileProcessed( remoteSnapshot.subList(0, 2).toArray(process));
		PriorityBlockingQueue<FileSnapshot> queue = (PriorityBlockingQueue<FileSnapshot>) new DirectFieldAccessor(
				backlog).getPropertyValue("backlog");
		Assert.assertEquals(1, queue.size());
		Assert.assertTrue(queue.contains(remoteSnapshot.get(2)));
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertEquals(1, queue.size());
		Assert.assertTrue(queue.contains(remoteSnapshot.get(2)));
		backlog.fileProcessed(remoteSnapshot.get(2));
		Assert.assertTrue(backlog.isEmpty());
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertTrue(backlog.isEmpty());
	}

	@Test
	public void testOneFileChangedSize() {
		PriorityBlockingQueue<FileSnapshot> queue = (PriorityBlockingQueue<FileSnapshot>) new DirectFieldAccessor(
				backlog).getPropertyValue("backlog");
		backlog.processSnapshot(remoteSnapshot);
		backlog.fileProcessed( remoteSnapshot.toArray(process));
		Assert.assertTrue(backlog.isEmpty());
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertTrue(backlog.isEmpty());
		remoteSnapshot.remove(2);
		FileSnapshot modifiedC = new FileSnapshot("c.txt", 1001, 112);
		remoteSnapshot.add(modifiedC);
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertEquals(1, queue.size());
		Assert.assertTrue(queue.contains(modifiedC));
	}

	@Test
	public void testOneFileChangedDate() {
		PriorityBlockingQueue<FileSnapshot> queue = (PriorityBlockingQueue<FileSnapshot>) new DirectFieldAccessor(
				backlog).getPropertyValue("backlog");

		backlog.processSnapshot(remoteSnapshot);
		backlog.fileProcessed( remoteSnapshot.toArray(process));
		Assert.assertTrue(backlog.isEmpty());
		backlog.processSnapshot(remoteSnapshot);
		remoteSnapshot.remove(2);
		FileSnapshot modifiedC = new FileSnapshot("c.txt", 1011, 102);
		remoteSnapshot.add(modifiedC);
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertEquals(1, queue.size());
		Assert.assertTrue(queue.contains(modifiedC));
	}

	@Test
	public void testOneFileAdded() {
		PriorityBlockingQueue<FileSnapshot> queue = (PriorityBlockingQueue<FileSnapshot>) new DirectFieldAccessor(
				backlog).getPropertyValue("backlog");
		backlog.processSnapshot(remoteSnapshot);
		backlog.fileProcessed( remoteSnapshot.toArray(process));
		Assert.assertTrue(backlog.isEmpty());
		backlog.processSnapshot(remoteSnapshot);
		FileSnapshot newD = new FileSnapshot("d.txt", 1003, 103);
		remoteSnapshot.add(newD);
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertEquals(1, queue.size());
		Assert.assertTrue(queue.contains(newD));
	}

	@Test
	public void testOneFileRemoved() {
		backlog.processSnapshot(remoteSnapshot);
		backlog.fileProcessed( remoteSnapshot.toArray(process));
		Assert.assertTrue(backlog.isEmpty());
		backlog.processSnapshot(remoteSnapshot);
		remoteSnapshot.remove(2);
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertTrue(backlog.isEmpty());
	}

	@Test
	public void testOneFileRemovedBeforeBeingProcessedInTheNextStep() {
		PriorityBlockingQueue<FileSnapshot> queue = (PriorityBlockingQueue<FileSnapshot>) new DirectFieldAccessor(
				backlog).getPropertyValue("backlog");
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertEquals(3, queue.size());
		remoteSnapshot.remove(2);
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertEquals(2, queue.size());
		backlog.processSnapshot(remoteSnapshot);
		Assert.assertEquals(2, queue.size());
	}

	// @Test selectForProcessing success/failure

	@Before
	public void generateInitialSnapshot() {
		this.remoteSnapshot = new ArrayList<FileSnapshot>();
		remoteSnapshot.add(new FileSnapshot("a.txt", 1000, 100));
		remoteSnapshot.add(new FileSnapshot("b.txt", 1001, 101));
		remoteSnapshot.add(new FileSnapshot("c.txt", 1002, 102));
	}

}
