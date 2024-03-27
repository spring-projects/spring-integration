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

package org.springframework.integration.xml.result;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;

import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.xml.DocumentBuilderFactoryUtils;

/**
 * @author Jonas Partner
 * @author Artem Bilan
 * @author Gary Russell
 * @author Christian Tzolov
 */
public class DomResultFactory implements ResultFactory {

	private final DocumentBuilderFactory documentBuilderFactory;

	private final Lock documentBuilderFactoryMonitor = new ReentrantLock();

	private final Lock lock = new ReentrantLock();

	public DomResultFactory() {
		this(DocumentBuilderFactoryUtils.newInstance());
		this.documentBuilderFactory.setNamespaceAware(true);
	}

	public DomResultFactory(DocumentBuilderFactory documentBuilderFactory) {
		Assert.notNull(documentBuilderFactory, "'documentBuilderFactory' must not be null.");
		this.documentBuilderFactory = documentBuilderFactory;
	}

	@Override
	public Result createResult(Object payload) {
		this.lock.lock();
		try {
			return new DOMResult(getNewDocumentBuilder().newDocument());
		}
		catch (ParserConfigurationException e) {
			throw new MessagingException("failed to create Result for payload type [" +
					payload.getClass().getName() + "]", e);
		}
		finally {
			this.lock.unlock();
		}
	}

	protected DocumentBuilder getNewDocumentBuilder() throws ParserConfigurationException {
		this.documentBuilderFactoryMonitor.lock();
		try {
			return this.documentBuilderFactory.newDocumentBuilder();
		}
		finally {
			this.documentBuilderFactoryMonitor.unlock();
		}
	}

}
