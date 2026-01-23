/*
 * Copyright 2015-present the original author or authors.
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

package org.springframework.integration.zip.config.xml;

import java.io.File;
import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.zip.transformer.UnZipTransformer;
import org.springframework.integration.zip.transformer.ZipResultType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 6.1
 */
@SpringJUnitConfig
@DirtiesContext
public class UnZipTransformerParserTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Test
	public void testUnZipTransformerParserWithDefaults() {
		EventDrivenConsumer consumer = this.context.getBean("unzipTransformerWithDefaults", EventDrivenConsumer.class);

		final AbstractMessageChannel inputChannel = TestUtils.getPropertyValue(consumer, "inputChannel");
		assertThat(inputChannel.getComponentName()).isEqualTo("input");

		final MessageTransformingHandler handler = TestUtils.getPropertyValue(consumer, "handler");

		assertThat(TestUtils.<String>getPropertyValue(handler, "outputChannelName")).isEqualTo("output");

		final UnZipTransformer unZipTransformer = TestUtils.getPropertyValue(handler, "transformer");

		final Charset charset = TestUtils.getPropertyValue(unZipTransformer, "charset");
		final ZipResultType zipResultType = TestUtils.getPropertyValue(unZipTransformer, "zipResultType");
		final File workDirectory = TestUtils.getPropertyValue(unZipTransformer, "workDirectory");
		final Boolean deleteFiles = TestUtils.getPropertyValue(unZipTransformer, "deleteFiles");
		final Boolean expectSingleResult = TestUtils.getPropertyValue(unZipTransformer, "expectSingleResult");

		assertThat(charset).isNotNull();
		assertThat(zipResultType).isNotNull();
		assertThat(workDirectory).isNotNull();
		assertThat(deleteFiles).isNotNull();
		assertThat(expectSingleResult).isNotNull();

		assertThat(charset).isEqualTo(Charset.defaultCharset());
		assertThat(zipResultType).isEqualTo(ZipResultType.FILE);
		assertThat(workDirectory)
				.isEqualTo(new File(System.getProperty("java.io.tmpdir") + File.separator + "ziptransformer"));
		assertThat(workDirectory.exists()).isTrue();
		assertThat(workDirectory.isDirectory()).isTrue();
		assertThat(deleteFiles).isFalse();
		assertThat(expectSingleResult).isFalse();
	}

	@Test
	public void testUnZipTransformerParserWithExplicitSettings() {
		EventDrivenConsumer consumer = this.context.getBean("unzipTransformer", EventDrivenConsumer.class);

		final AbstractMessageChannel inputChannel = TestUtils.getPropertyValue(consumer, "inputChannel");
		assertThat(inputChannel.getComponentName()).isEqualTo("input");

		final MessageTransformingHandler handler = TestUtils.getPropertyValue(consumer, "handler");

		assertThat(TestUtils.<String>getPropertyValue(handler, "outputChannelName")).isEqualTo("output");

		final UnZipTransformer unZipTransformer = TestUtils.getPropertyValue(handler, "transformer");

		final Charset charset = TestUtils.getPropertyValue(unZipTransformer, "charset");
		final ZipResultType zipResultType = TestUtils.getPropertyValue(unZipTransformer, "zipResultType");
		final File workDirectory = TestUtils.getPropertyValue(unZipTransformer, "workDirectory");
		final Boolean deleteFiles = TestUtils.getPropertyValue(unZipTransformer, "deleteFiles");
		final Boolean expectSingleResult = TestUtils.getPropertyValue(unZipTransformer, "expectSingleResult");

		assertThat(charset).isNotNull();
		assertThat(zipResultType).isNotNull();
		assertThat(workDirectory).isNotNull();
		assertThat(deleteFiles).isNotNull();
		assertThat(expectSingleResult).isNotNull();

		assertThat(charset).isEqualTo(Charset.defaultCharset());
		assertThat(zipResultType).isEqualTo(ZipResultType.FILE);
		assertThat(workDirectory)
				.isEqualTo(new File(System.getProperty("java.io.tmpdir") + File.separator + "ziptransformer"));
		assertThat(workDirectory.exists()).isTrue();
		assertThat(workDirectory.isDirectory()).isTrue();
		assertThat(deleteFiles).isTrue();
		assertThat(expectSingleResult).isTrue();
	}

}
