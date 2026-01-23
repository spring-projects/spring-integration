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
import java.util.zip.Deflater;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.zip.transformer.ZipResultType;
import org.springframework.integration.zip.transformer.ZipTransformer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 6.1
 */
@SpringJUnitConfig
@DirtiesContext
public class ZipTransformerParserTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Test
	public void testZipTransformerParserWithDefaults() {
		EventDrivenConsumer consumer = this.context.getBean("zipTransformerWithDefaults", EventDrivenConsumer.class);

		final AbstractMessageChannel inputChannel = TestUtils.getPropertyValue(consumer, "inputChannel");
		assertThat(inputChannel.getComponentName()).isEqualTo("input");

		final MessageTransformingHandler handler = TestUtils.getPropertyValue(consumer, "handler");

		assertThat(TestUtils.<String>getPropertyValue(handler, "outputChannelName")).isEqualTo("output");

		final ZipTransformer zipTransformer = TestUtils.getPropertyValue(handler, "transformer");

		final Charset charset = TestUtils.getPropertyValue(zipTransformer, "charset");
		final FileNameGenerator fileNameGenerator = TestUtils.getPropertyValue(zipTransformer, "fileNameGenerator");
		final ZipResultType zipResultType = TestUtils.getPropertyValue(zipTransformer, "zipResultType");
		final File workDirectory = TestUtils.getPropertyValue(zipTransformer, "workDirectory");
		final Integer compressionLevel = TestUtils.getPropertyValue(zipTransformer, "compressionLevel");
		final Boolean deleteFiles = TestUtils.getPropertyValue(zipTransformer, "deleteFiles");

		assertThat(charset).isNotNull();
		assertThat(fileNameGenerator).isNotNull();
		assertThat(zipResultType).isNotNull();
		assertThat(workDirectory).isNotNull();
		assertThat(deleteFiles).isNotNull();
		assertThat(compressionLevel).isNotNull();

		assertThat(charset).isEqualTo(Charset.defaultCharset());
		assertThat(fileNameGenerator).isInstanceOf(DefaultFileNameGenerator.class);
		assertThat(zipResultType).isEqualTo(ZipResultType.FILE);
		assertThat(workDirectory)
				.isEqualTo(new File(System.getProperty("java.io.tmpdir") + File.separator + "ziptransformer"));
		assertThat(workDirectory.exists()).isTrue();
		assertThat(workDirectory.isDirectory()).isTrue();
		assertThat(deleteFiles).isFalse();
		assertThat(compressionLevel).isEqualTo(Deflater.DEFAULT_COMPRESSION);
	}

	@Test
	public void testZipTransformerParserWithExplicitSettings() {
		EventDrivenConsumer consumer = this.context.getBean("zipTransformer", EventDrivenConsumer.class);

		final AbstractMessageChannel inputChannel = TestUtils.getPropertyValue(consumer, "inputChannel");
		assertThat(inputChannel.getComponentName()).isEqualTo("input");

		final MessageTransformingHandler handler = TestUtils.getPropertyValue(consumer, "handler");

		assertThat(TestUtils.<String>getPropertyValue(handler, "outputChannelName")).isEqualTo("output");

		final ZipTransformer zipTransformer = TestUtils.getPropertyValue(handler, "transformer");

		final Charset charset = TestUtils.getPropertyValue(zipTransformer, "charset");
		final FileNameGenerator fileNameGenerator = TestUtils.getPropertyValue(zipTransformer, "fileNameGenerator");
		final ZipResultType zipResultType = TestUtils.getPropertyValue(zipTransformer, "zipResultType");
		final File workDirectory = TestUtils.getPropertyValue(zipTransformer, "workDirectory");
		final Integer compressionLevel = TestUtils.getPropertyValue(zipTransformer, "compressionLevel");
		final Boolean deleteFiles = TestUtils.getPropertyValue(zipTransformer, "deleteFiles");

		assertThat(charset).isNotNull();
		assertThat(fileNameGenerator).isNotNull();
		assertThat(zipResultType).isNotNull();
		assertThat(workDirectory).isNotNull();
		assertThat(deleteFiles).isNotNull();
		assertThat(compressionLevel).isNotNull();

		assertThat(charset).isEqualTo(Charset.defaultCharset());
		assertThat(fileNameGenerator).isInstanceOf(DefaultFileNameGenerator.class);
		assertThat(zipResultType).isEqualTo(ZipResultType.BYTE_ARRAY);
		assertThat(workDirectory)
				.isEqualTo(new File(System.getProperty("java.io.tmpdir") + File.separator + "ziptransformer"));
		assertThat(workDirectory.exists()).isTrue();
		assertThat(workDirectory.isDirectory()).isTrue();
		assertThat(compressionLevel).isEqualTo(2);
		assertThat(deleteFiles).isTrue();
	}

	@Test
	public void testZipTransformerParserWithIncorrectResultType() {

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("ZipTransformerParserTestsWithIncorrectResultType.xml",
								getClass()))
				.withMessageContaining("Failed to convert property value of type 'java.lang.String' " +
						"to required type 'org.springframework.integration.zip.transformer.ZipResultType'");
	}

}
