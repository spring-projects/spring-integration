/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.security.config;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class IncludeExcludePatternParser {

	public OrderedIncludeExcludeList createFromNodeList(boolean includeByDefault, NodeList nodeList) {
		List<IncludeExcludePattern> patterns = new ArrayList<IncludeExcludePattern>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i).getNodeName().equals("includePattern")) {
				patterns.add(new IncludeExcludePattern(true, ((Element) nodeList.item(i)).getTextContent()));
			}
			else if (nodeList.item(i).getNodeName().equals("excludePattern")) {
				patterns.add(new IncludeExcludePattern(false, ((Element) nodeList.item(i)).getTextContent()));
			}
		}
		return new JdkRegExpOrderedIncludeExcludeList(includeByDefault, patterns);
	}

}
