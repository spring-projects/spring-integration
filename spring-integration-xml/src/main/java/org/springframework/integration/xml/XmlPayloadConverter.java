/*
 * Copyright 2002-2019 the original author or authors.
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
