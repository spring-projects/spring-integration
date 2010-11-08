/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.file.entries;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Filters a listing of entries (T) by qualifying their 'name' (as determined by {@link org.springframework.integration.file.entries.EntryNameExtractor})
 * against a regular expression (an instance of {@link java.util.regex.Pattern})
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @param <T>   the type of entry
 * @since 2.0
 */
public class PatternMatchingEntryListFilter<T> extends AbstractEntryListFilter<T> implements InitializingBean {

	private volatile EntryNameExtractor<T> entryNameExtractor;

	private volatile Pattern pattern;

	private volatile String patternExpression;


	public PatternMatchingEntryListFilter(EntryNameExtractor<T> entryNameExtractor, String pattern) {
		this.entryNameExtractor = entryNameExtractor;
		this.patternExpression = pattern;
	}

	public PatternMatchingEntryListFilter(EntryNameExtractor<T> entryNameExtractor, Pattern pattern) {
		this.entryNameExtractor = entryNameExtractor;
		this.pattern = pattern;
	}


	public void setEntryNameExtractor(EntryNameExtractor<T> entryNameExtractor) {
		this.entryNameExtractor = entryNameExtractor;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public void setPatternExpression(String patternExpression) {
		this.patternExpression = patternExpression;
	}

	public void afterPropertiesSet() throws Exception {
		if (StringUtils.hasText(this.patternExpression) && (pattern == null)) {
			this.pattern = Pattern.compile(this.patternExpression);
		}
		Assert.notNull(this.entryNameExtractor, "'entryNameExtractor' must not be null!");
		Assert.notNull(this.pattern, "'pattern' must not be null!");
	}

	@Override
	public boolean accept(T entry) {
		return (entry != null) && this.pattern.matcher(this.entryNameExtractor.getName(entry)).matches();
	}

}
