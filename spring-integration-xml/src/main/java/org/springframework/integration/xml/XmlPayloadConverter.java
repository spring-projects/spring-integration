/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml;

import javax.xml.transform.Source;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Converter for creating XML {@link Document}, {@link Node} or {@link Source}
 * instances from other types (e.g. String).
 *
 * @author Jonas Partner
 */
public interface XmlPayloadConverter {

	Document convertToDocument(Object object);

	Node convertToNode(Object object);

	Source convertToSource(Object object);

}
