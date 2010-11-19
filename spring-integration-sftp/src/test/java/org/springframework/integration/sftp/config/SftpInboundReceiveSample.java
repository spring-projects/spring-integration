/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.sftp.config;

import static junit.framework.Assert.assertTrue;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Oleg Zhurakousy
 *
 */
public class SftpInboundReceiveSample {

	@Test
	@Ignore
	public void testInbound() throws Exception{
		new ClassPathXmlApplicationContext("SftpInboundReceiveSample-ignored-context.xml", SftpInboundReceiveSample.class);
		Thread.sleep(3000);
		File fileA = new File("local-test-dir/a.test");
		File fileB = new File("local-test-dir/a.test");
		assertTrue(new File("local-test-dir/a.test").exists());
		assertTrue(new File("local-test-dir/b.test").exists());
		System.out.println("Done");
	}

}
