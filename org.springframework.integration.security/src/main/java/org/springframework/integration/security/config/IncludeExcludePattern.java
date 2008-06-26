package org.springframework.integration.security.config;

/**
 * 
 * @author Jonas Partner
 *
 */
public class IncludeExcludePattern {
	
	private final boolean isIncludePattern;
	
	private final String pattern;

	public IncludeExcludePattern(boolean isIncludePattern, String pattern) {
		this.isIncludePattern = isIncludePattern;
		this.pattern = pattern;
	}

	public IncludeExcludePattern(String pattern) {
		this(true,pattern);
	}
	
	public boolean isIncludePattern() {
		return isIncludePattern;
	}

	public String getPattern() {
		return pattern;
	}
	
}
