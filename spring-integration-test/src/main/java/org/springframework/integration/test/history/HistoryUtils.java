/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.test.history;

import java.util.Properties;

import org.springframework.integration.history.MessageHistory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 */
public abstract class HistoryUtils {

	/**
	 * @param history a message history
	 * @param componentName the name of a component to scan for
	 * @param startingIndex the index to start scanning
	 * @return the properties provided by the named component or null if none available
	 */
	public static Properties locateComponentInHistory(MessageHistory history, String componentName, int startingIndex){
		Assert.notNull(history, "'history' must not be null");
		Assert.isTrue(StringUtils.hasText(componentName), "'componentName' must be provided");
		Assert.isTrue(startingIndex < history.size(), "'startingIndex' can not be greater then size of history");
		Properties component = null;
		for (int i = startingIndex; i < history.size(); i++) {
			Properties properties = history.get(i);
			if (componentName.equals(properties.get("name"))){
				component = properties;
				break;
			}
		}
		return component;
	}

}
