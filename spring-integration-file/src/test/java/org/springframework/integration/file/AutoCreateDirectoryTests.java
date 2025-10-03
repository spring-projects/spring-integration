/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.file.inbound.FileReadingMessageSource;
import org.springframework.integration.file.outbound.FileWritingMessageHandler;
import org.springframework.integration.test.support.TestApplicationContextAware;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 1.0.3
 */
public class AutoCreateDirectoryTests implements TestApplicationContextAware {

	private static final String BASE_PATH =
			System.getProperty("java.io.tmpdir") + File.separator + "AutoCreateDirectoryTests";

	private static final String INBOUND_PATH = BASE_PATH + File.separator + "inbound";

	private static final String OUTBOUND_PATH = BASE_PATH + File.separator + "outbound";

	@BeforeEach
	@AfterEach
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
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		source.afterPropertiesSet();
		source.start();
		assertThat(new File(INBOUND_PATH)).exists();
		source.stop();
	}

	@Test
	public void autoCreateForInboundDisabled() {
		FileReadingMessageSource source = new FileReadingMessageSource();
		source.setDirectory(new File(INBOUND_PATH));
		source.setAutoCreateDirectory(false);
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		source.afterPropertiesSet();
		assertThatIllegalArgumentException()
				.isThrownBy(source::start);
	}

	@Test
	public void autoCreateForOutboundEnabledByDefault() {
		FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(OUTBOUND_PATH));
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.afterPropertiesSet();
		assertThat(new File(OUTBOUND_PATH)).exists();
	}

	@Test
	public void autoCreateForOutboundDisabled() {
		FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(OUTBOUND_PATH));
		handler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		handler.setAutoCreateDirectory(false);
		assertThatIllegalArgumentException()
				.isThrownBy(handler::afterPropertiesSet);
	}

}
