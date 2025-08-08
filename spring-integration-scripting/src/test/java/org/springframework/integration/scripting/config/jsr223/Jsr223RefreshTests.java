/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting.config.jsr223;

import java.beans.PropertyEditorSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
 * @author David Turanski
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class Jsr223RefreshTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Test
	public void referencedScript() throws Exception {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		this.referencedScriptInput.send(MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build());
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("ruby-test-1");
		this.referencedScriptInput.send(MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build());
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("ruby-test-0");
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

		private final String[] scripts = {"\"ruby-#{payload}-0\"", "\"ruby-#{payload}-1\""};

		CycleResource() {
			super();
		}

		@Override
		public String getDescription() {
			return "CycleResource";
		}

		@Override
		public String getFilename() throws IllegalStateException {
			return "CycleResource";
		}

		@Override
		public long lastModified() throws IOException {
			return -1;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			if (++count > scripts.length - 1) {
				count = 0;
			}
			return new ByteArrayInputStream(scripts[count].getBytes());
		}

	}

}
