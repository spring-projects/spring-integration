/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.source;

import java.io.File;
import java.io.FileReader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.springframework.xml.transform.TransformerFactoryUtils;

/**
 * {@link SourceFactory} implementation which supports creation of a {@link StringSource}
 * from a {@link Document}, {@link File} or {@link String} payload
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Christian Tzolov
 */
public class StringSourceFactory implements SourceFactory {

	private final Lock lock = new ReentrantLock();

	private final TransformerFactory transformerFactory;

	public StringSourceFactory() {
		this(TransformerFactoryUtils.newInstance());
	}

	public StringSourceFactory(TransformerFactory transformerFactory) {
		Assert.notNull(transformerFactory, "'transformerFactory' must not be null.");
		this.transformerFactory = transformerFactory;
	}

	public Source createSource(Object payload) {
		Source source = null;
		if (payload instanceof String) {
			source = new StringSource((String) payload);
		}
		else if (payload instanceof Document) {
			source = createStringSourceForDocument((Document) payload);
		}
		else if (payload instanceof File) {
			source = createStringSourceForFile((File) payload);
		}
		if (source == null) {
			throw new MessagingException("Failed to create Source for payload type ["
					+ payload.getClass().getName() + "]");
		}
		return source;
	}

	private StringSource createStringSourceForDocument(Document document) {
		try {
			StringResult result = new StringResult();
			Transformer transformer = getTransformer();
			transformer.transform(new DOMSource(document), result);
			return new StringSource(result.toString());
		}
		catch (Exception e) {
			throw new MessagingException("failed to create StringSource from document", e);
		}
	}

	private StringSource createStringSourceForFile(File file) {
		try {
			String content = FileCopyUtils.copyToString(new FileReader(file));
			return new StringSource(content);
		}
		catch (Exception e) {
			throw new MessagingException("failed to create StringSource from file", e);
		}
	}

	private Transformer getTransformer() {
		this.lock.lock();
		try {
			return this.transformerFactory.newTransformer();
		}
		catch (Exception e) {
			throw new MessagingException("Exception creating transformer", e);
		}
		finally {
			this.lock.unlock();
		}
	}

}
