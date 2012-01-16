/**
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
package org.springframework.integration.smb;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Markus Spann
 *
 */
public class SmbParserInboundTests extends AbstractBaseTest {

	@Before
	public void prepare() {
		ensureExists("test-temp/remote-10");
		cleanUp();
	}

	@Test
	public void testLocalFilesAutoCreationTrue() throws Exception {
		assertFileNotExists(new File("test-temp/local-10"));
		new ClassPathXmlApplicationContext(getApplicationContextXmlFile(), this.getClass());
		assertFileExists(new File("test-temp/local-10"));
		assertFileNotExists(new File("test-temp/local-6"));
	}

	@Test(expected = BeanCreationException.class)
	public void testLocalFilesAutoCreationFalse() throws Exception {
		assertFileNotExists(new File("test-temp/local-6"));
        new ClassPathXmlApplicationContext(getApplicationContextXmlFile("-fail"), this.getClass());
	}

	@After
	public void cleanUp() {
		delete("test-temp/local-10", "test-temp/local-6");
	}

	public static void main(String[] _args) {
		new SmbParserInboundTests().cleanUp();
		runTests(SmbParserInboundTests.class, "testLocalFilesAutoCreationTrue", "testLocalFilesAutoCreationFalse");
	}

}
