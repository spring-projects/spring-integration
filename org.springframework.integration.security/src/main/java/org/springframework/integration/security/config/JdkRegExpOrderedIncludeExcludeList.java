package org.springframework.integration.security.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class JdkRegExpOrderedIncludeExcludeList implements OrderedIncludeExcludeList {
	
	private final boolean includeByDefault;
	
	private final List<PatternHolder> patternHolders;
	
	public JdkRegExpOrderedIncludeExcludeList(List<IncludeExcludePattern> patterns){
		this(true, patterns);
	}
	
	public JdkRegExpOrderedIncludeExcludeList(boolean includeByDefault, List<IncludeExcludePattern> patterns) {
		super();
		this.includeByDefault = includeByDefault;
		List<PatternHolder> patternHolders = new ArrayList<PatternHolder>();
		for(int i = 0 ; i <patterns.size(); i++){
			patternHolders.add(new PatternHolder(Pattern.compile(patterns.get(i).getPattern()), patterns.get(i)) );
		}
		this.patternHolders = Collections.unmodifiableList(patternHolders);
	}

	public boolean isIncluded(String name) {
		for(int i = 0; i < patternHolders.size(); i++){
			if(patternHolders.get(i).compiledPattern.matcher(name).matches()){
				return (patternHolders.get(i).includeExcludePattern.isIncludePattern());
			}
		}
		return includeByDefault;
	}
	
	private static class PatternHolder{
		
		private final Pattern compiledPattern;
		
		private final IncludeExcludePattern includeExcludePattern;

		public PatternHolder(Pattern compiledPattern, IncludeExcludePattern includeExcludePattern) {
			super();
			this.compiledPattern = compiledPattern;
			this.includeExcludePattern = includeExcludePattern;
		}

		
	}
	
	

	
	
}
