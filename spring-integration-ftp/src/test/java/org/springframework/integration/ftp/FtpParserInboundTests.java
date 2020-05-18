/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.ftp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
public class FtpParserInboundTests {

	@BeforeEach
	public void prepare() {
		new File("target/foo").delete();
	}

	@Test
	public void testLocalFilesAutoCreationTrue() throws Exception {
		assertThat(!new File("target/foo").exists()).isTrue();
		new ClassPathXmlApplicationContext("FtpParserInboundTests-context.xml", this.getClass()).close();
		assertThat(new File("target/foo").exists()).isTrue();
		assertThat(!new File("target/bar").exists()).isTrue();
	}
	@Test
	public void testLocalFilesAutoCreationFalse() throws Exception {
		assertThat(!new File("target/bar").exists()).isTrue();
		try {
			new ClassPathXmlApplicationContext("FtpParserInboundTests-fail-context.xml", this.getClass()).close();
			fail("BeansException expected.");
		}
		catch (BeansException e) {
			assertThat(e).isInstanceOf(BeanCreationException.class);
			Throwable cause = e.getCause();
			assertThat(cause).isInstanceOf(BeanInitializationException.class);
			cause = cause.getCause();
			assertThat(cause).isInstanceOf(FileNotFoundException.class);
			assertThat(cause.getMessage()).isEqualTo("bar");
		}
	}

	@AfterEach
	public void cleanUp() throws Exception {
		new File("target/foo").delete();
	}
}
