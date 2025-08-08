/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.groovy.config;

import java.beans.PropertyEditorSupport;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.AbstractResource;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GroovyRefreshTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Test
	public void referencedScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		this.referencedScriptInput.send(MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build());
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("groovy-test-1");
		this.referencedScriptInput.send(MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build());
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("groovy-test-0");
		assertThat(replyChannel.receive(0)).isNull();
	}

	public static class ResourceEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			super.setValue(new CycleResource());
		}

	}

	private static class CycleResource extends AbstractResource {

		private int count = -1;

		private String[] scripts =
				{"\"groovy-${binding.variables['payload']}-0\"",
						"\"groovy-${binding.variables['payload']}-1\""};

		public String getDescription() {
			return "CycleResource";
		}

		@Override
		public String getFilename() throws IllegalStateException {
			return "CycleResource";
		}

		@Override
		public long lastModified() {
			return -1;
		}

		public InputStream getInputStream() {
			if (++this.count > this.scripts.length - 1) {
				this.count = 0;
			}
			return new ByteArrayInputStream(this.scripts[count].getBytes());
		}

	}

}
