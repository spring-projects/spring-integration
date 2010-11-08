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

package org.springframework.integration.file.filters;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.file.entries.EntryNameExtractor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Filters a listing of files by qualifying their 'name' (as determined by {@link org.springframework.integration.file.entries.EntryNameExtractor})
 * against a regular expression (an instance of {@link java.util.regex.Pattern})
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @param <F> the type of file entry
 * @since 2.0
 */
public class PatternMatchingFileListFilter<F> extends AbstractFileListFilter<F> implements InitializingBean {

	private volatile EntryNameExtractor<F> entryNameExtractor;

	private volatile Pattern pattern;

	private volatile String patternExpression;


	public PatternMatchingFileListFilter(EntryNameExtractor<F> entryNameExtractor, String pattern) {
		this.entryNameExtractor = entryNameExtractor;
		this.patternExpression = pattern;
	}

	public PatternMatchingFileListFilter(EntryNameExtractor<F> entryNameExtractor, Pattern pattern) {
		this.entryNameExtractor = entryNameExtractor;
		this.pattern = pattern;
	}


	public void setEntryNameExtractor(EntryNameExtractor<F> entryNameExtractor) {
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
	public boolean accept(F entry) {
		return (entry != null) && this.pattern.matcher(this.entryNameExtractor.getName(entry)).matches();
	}

}
