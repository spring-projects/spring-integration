/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.zip.config.xml;

import java.io.File;
import java.io.IOException;
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
	public void testUnZipTransformerParserWithDefaults() throws IOException {
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
		Long maxUncompressedSize = (Long) TestUtils.getPropertyValue(unZipTransformer, "maxUncompressedSize");
		Integer maxEntryCount = (Integer) TestUtils.getPropertyValue(unZipTransformer, "maxEntryCount");
		Double maxCompressionRatio = (Double) TestUtils.getPropertyValue(unZipTransformer, "maxCompressionRatio");

		assertThat(charset).isNotNull();
		assertThat(zipResultType).isNotNull();
		assertThat(workDirectory).isNotNull();
		assertThat(deleteFiles).isNotNull();
		assertThat(expectSingleResult).isNotNull();

		assertThat(charset).isEqualTo(Charset.defaultCharset());
		assertThat(zipResultType).isEqualTo(ZipResultType.FILE);
		assertThat(workDirectory)
				.isEqualTo(new File(System.getProperty("java.io.tmpdir") + File.separator + "ziptransformer")
						.getCanonicalFile());
		assertThat(workDirectory.exists()).isTrue();
		assertThat(workDirectory.isDirectory()).isTrue();
		assertThat(deleteFiles).isFalse();
		assertThat(expectSingleResult).isFalse();
		assertThat(maxUncompressedSize).isEqualTo(1024 * 1024 * 100);
		assertThat(maxEntryCount).isEqualTo(1000);
		assertThat(maxCompressionRatio).isEqualTo(100);
	}

	@Test
	public void testUnZipTransformerParserWithExplicitSettings() throws IOException {
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
				.isEqualTo(new File(System.getProperty("java.io.tmpdir") + File.separator + "ziptransformer")
						.getCanonicalFile());
		assertThat(workDirectory.exists()).isTrue();
		assertThat(workDirectory.isDirectory()).isTrue();
		assertThat(deleteFiles).isTrue();
		assertThat(expectSingleResult).isTrue();
	}

	@Test
	public void testUnZipTransformerParserWithSizeConstraints() {
		EventDrivenConsumer consumer =
				this.context.getBean("unzipTransformerWithSizeConstraints", EventDrivenConsumer.class);

		UnZipTransformer unZipTransformer = (UnZipTransformer) TestUtils.getPropertyValue(consumer,
				"handler.transformer");

		Long maxUncompressedSize = (Long) TestUtils.getPropertyValue(unZipTransformer, "maxUncompressedSize");
		Integer maxEntryCount = (Integer) TestUtils.getPropertyValue(unZipTransformer, "maxEntryCount");
		Double maxCompressionRatio = (Double) TestUtils.getPropertyValue(unZipTransformer, "maxCompressionRatio");

		assertThat(maxUncompressedSize).isEqualTo(2048);
		assertThat(maxEntryCount).isEqualTo(5);
		assertThat(maxCompressionRatio).isEqualTo(10.5);
	}

}
