/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.file.transformer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.transformer.Transformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Base class for transformers that convert a File payload.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public abstract class AbstractFilePayloadTransformer<T> implements Transformer, BeanFactoryAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile boolean deleteFiles;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private boolean messageBuilderFactorySet;

	private volatile BeanFactory beanFactory;

	/**
	 * Specify whether to delete the File after transformation.
	 * Default is {@code false}.
	 * @param deleteFiles true to delete the file.
	 */
	public void setDeleteFiles(boolean deleteFiles) {
		this.deleteFiles = deleteFiles;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (!this.messageBuilderFactorySet) {
			if (this.beanFactory != null) {
				this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
			}
			this.messageBuilderFactorySet = true;
		}
		return this.messageBuilderFactory;
	}

	@Override
	public final Message<?> transform(Message<?> message) {
		try {
			Assert.notNull(message, "Message must not be null");
			Object payload = message.getPayload();
			Assert.notNull(payload, "Message payload must not be null");
			Assert.isInstanceOf(File.class, payload, "Message payload must be of type [java.io.File]");
			File file = (File) payload;
			T result = transformFile(file);
			Message<?> transformedMessage =
					getMessageBuilderFactory()
							.withPayload(result)
							.copyHeaders(message.getHeaders())
							.setHeaderIfAbsent(FileHeaders.ORIGINAL_FILE, file)
							.setHeaderIfAbsent(FileHeaders.FILENAME, file.getName())
							.build();
			if (this.deleteFiles && !file.delete() && this.logger.isWarnEnabled()) {
				this.logger.warn("failed to delete File '" + file + "'");
			}
			return transformedMessage;
		}
		catch (Exception ex) {
			throw new MessagingException(message, "failed to transform File Message", ex);
		}
	}

	/**
	 * Subclasses must implement this method to transform the File contents.
	 * @param file The file.
	 * @return The result of the transformation.
	 * @throws IOException Any IOException.
	 */
	protected abstract T transformFile(File file) throws IOException;

}
