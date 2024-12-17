/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.integration.smb;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Markus Spann
 * @author Prafull Kumar Soni
 * @author Artem Bilan
 *
 */
public class SmbParserInboundTests extends AbstractBaseTests {

	@BeforeEach
	public void prepare() {
		ensureExists("test-temp/remote-10");
		cleanUp();
	}

	@Test
	@SuppressWarnings("try")
	public void testLocalFilesAutoCreationTrue() {
		assertFileNotExists(new File("test-temp/local-10"));
		try (var ctx = new ClassPathXmlApplicationContext(getApplicationContextXmlFile(), getClass())) {
			assertFileExists(new File("test-temp/local-10"));
			assertFileNotExists(new File("test-temp/local-6"));
		}
	}

	@Test
	public void testLocalFilesAutoCreationFalse() {
		assertFileNotExists(new File("test-temp/local-6"));
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(getApplicationContextXmlFile("-fail"), getClass()));
	}

	@AfterEach
	public void cleanUp() {
		delete("test-temp/local-10", "test-temp/local-6");
	}

}
