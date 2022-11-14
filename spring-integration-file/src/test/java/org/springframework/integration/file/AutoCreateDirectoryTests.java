/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.file;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 1.0.3
 */
public class AutoCreateDirectoryTests {

	private static final String BASE_PATH =
			System.getProperty("java.io.tmpdir") + File.separator + "AutoCreateDirectoryTests";

	private static final String INBOUND_PATH = BASE_PATH + File.separator + "inbound";

	private static final String OUTBOUND_PATH = BASE_PATH + File.separator + "outbound";

	@Before
	@After
	public void clearDirectories() {
		File baseDir = new File(BASE_PATH);
		File inboundDir = new File(INBOUND_PATH);
		File outboundDir = new File(OUTBOUND_PATH);
		if (inboundDir.exists()) {
			inboundDir.delete();
		}
		if (outboundDir.exists()) {
			outboundDir.delete();
		}
		if (baseDir.exists()) {
			baseDir.delete();
		}
	}

	@Test
	public void autoCreateForInboundEnabledByDefault() {
		FileReadingMessageSource source = new FileReadingMessageSource();
		source.setDirectory(new File(INBOUND_PATH));
		source.setBeanFactory(mock(BeanFactory.class));
		source.afterPropertiesSet();
		source.start();
		assertThat(new File(INBOUND_PATH).exists()).isTrue();
		source.stop();
	}

	@Test
	public void autoCreateForInboundDisabled() {
		FileReadingMessageSource source = new FileReadingMessageSource();
		source.setDirectory(new File(INBOUND_PATH));
		source.setAutoCreateDirectory(false);
		source.setBeanFactory(mock(BeanFactory.class));
		source.afterPropertiesSet();
		assertThatIllegalArgumentException()
				.isThrownBy(source::start);
	}

	@Test
	public void autoCreateForOutboundEnabledByDefault() {
		FileWritingMessageHandler handler = new FileWritingMessageHandler(
				new File(OUTBOUND_PATH));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		assertThat(new File(OUTBOUND_PATH).exists()).isTrue();
	}

	@Test(expected = IllegalArgumentException.class)
	public void autoCreateForOutboundDisabled() {
		FileWritingMessageHandler handler = new FileWritingMessageHandler(
				new File(OUTBOUND_PATH));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setAutoCreateDirectory(false);
		handler.afterPropertiesSet();
	}

}
