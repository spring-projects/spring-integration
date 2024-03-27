/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.file.config;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class AutoCreateDirectoryIntegrationTests {

	private static final String BASE_PATH =
			System.getProperty("java.io.tmpdir") + File.separator +
					AutoCreateDirectoryIntegrationTests.class.getSimpleName();

	@Autowired
	private ApplicationContext context;

	@BeforeClass
	public static void setupNonAutoCreatedDirectories() {
		new File(BASE_PATH).delete();
		new File(BASE_PATH + File.separator + "customInbound").mkdirs();
		new File(BASE_PATH + File.separator + "customOutbound").mkdirs();
		new File(BASE_PATH + File.separator + "customOutboundGateway").mkdirs();
	}

	@AfterClass
	public static void deleteBaseDirectory() {
		new File(BASE_PATH).delete();
	}

	@Test
	public void defaultInbound() {
		Object adapter = context.getBean("defaultInbound");
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		FileReadingMessageSource source = (FileReadingMessageSource)
				adapterAccessor.getPropertyValue("source");
		assertThat(new DirectFieldAccessor(source).getPropertyValue("autoCreateDirectory")).isEqualTo(Boolean.TRUE);
		source.start();
		assertThat(new File(BASE_PATH + File.separator + "defaultInbound").exists()).isTrue();
	}

	@Test
	public void customInbound() {
		Object adapter = context.getBean("customInbound");
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		FileReadingMessageSource source = (FileReadingMessageSource)
				adapterAccessor.getPropertyValue("source");
		assertThat(new File(BASE_PATH + File.separator + "customInbound").exists()).isTrue();
		assertThat(new DirectFieldAccessor(source).getPropertyValue("autoCreateDirectory")).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void defaultOutbound() {
		Object adapter = context.getBean("defaultOutbound");
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				adapterAccessor.getPropertyValue("handler");
		assertThat(new DirectFieldAccessor(handler).getPropertyValue("autoCreateDirectory")).isEqualTo(Boolean.TRUE);
		assertThat(new File(BASE_PATH + File.separator + "defaultOutbound").exists()).isTrue();
	}

	@Test
	public void customOutbound() {
		Object adapter = context.getBean("customOutbound");
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				adapterAccessor.getPropertyValue("handler");
		assertThat(new File(BASE_PATH + File.separator + "customOutbound").exists()).isTrue();
		assertThat(new DirectFieldAccessor(handler).getPropertyValue("autoCreateDirectory")).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void defaultOutboundGateway() {
		Object gateway = context.getBean("defaultOutboundGateway");
		DirectFieldAccessor gatewayAccessor = new DirectFieldAccessor(gateway);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				gatewayAccessor.getPropertyValue("handler");
		assertThat(new DirectFieldAccessor(handler).getPropertyValue("autoCreateDirectory")).isEqualTo(Boolean.TRUE);
		assertThat(new File(BASE_PATH + File.separator + "defaultOutboundGateway").exists()).isTrue();
	}

	@Test
	public void customOutboundGateway() {
		Object gateway = context.getBean("customOutboundGateway");
		DirectFieldAccessor gatewayAccessor = new DirectFieldAccessor(gateway);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				gatewayAccessor.getPropertyValue("handler");
		assertThat(new File(BASE_PATH + File.separator + "customOutboundGateway").exists()).isTrue();
		assertThat(new DirectFieldAccessor(handler).getPropertyValue("autoCreateDirectory")).isEqualTo(Boolean.FALSE);
	}

}
