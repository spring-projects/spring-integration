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
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class JdkRegExpOrderedIncludeExcludeList implements OrderedIncludeExcludeList {

	private final boolean includeByDefault;

	private final List<PatternHolder> patternHolders;

	public JdkRegExpOrderedIncludeExcludeList(List<IncludeExcludePattern> patterns) {
		this(true, patterns);
	}

	public JdkRegExpOrderedIncludeExcludeList(boolean includeByDefault, List<IncludeExcludePattern> patterns) {
		super();
		this.includeByDefault = includeByDefault;
		List<PatternHolder> patternHolders = new ArrayList<PatternHolder>();
		for (int i = 0; i < patterns.size(); i++) {
			patternHolders.add(new PatternHolder(Pattern.compile(patterns.get(i).getPattern()), patterns.get(i)));
		}
		this.patternHolders = Collections.unmodifiableList(patternHolders);
	}

	public boolean isIncluded(String name) {
		for (int i = 0; i < patternHolders.size(); i++) {
			if (patternHolders.get(i).compiledPattern.matcher(name).matches()) {
				return (patternHolders.get(i).includeExcludePattern.isIncludePattern());
			}
		}
		return includeByDefault;
	}

	private static class PatternHolder {

		private final Pattern compiledPattern;

		private final IncludeExcludePattern includeExcludePattern;

		public PatternHolder(Pattern compiledPattern, IncludeExcludePattern includeExcludePattern) {
			super();
			this.compiledPattern = compiledPattern;
			this.includeExcludePattern = includeExcludePattern;
		}

	}

}
