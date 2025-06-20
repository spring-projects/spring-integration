/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.xml.config;

import java.util.LinkedList;

import javax.xml.transform.Source;

import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

/**
 *
 * @author Jonas Partner
 * @author Artem Bilan
 *
 */
public class StubUnmarshaller implements Unmarshaller {

	public final LinkedList<Source> sourcesPassed = new LinkedList<>();

	public boolean supports(Class<?> clazz) {
		return true;
	}

	public Object unmarshal(Source source) throws XmlMappingException {
		this.sourcesPassed.addFirst(source);
		return "unmarshalled";
	}

}
