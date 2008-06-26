package org.springframework.integration.security.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


/**
 * 
 * @author Jonas Partner
 *
 */
public class JdkRegExpOrderedIncludeExcludeListTests {

	@Test
	public void testSimpleInclusionIncludeByDefaultFalse() {
		List<IncludeExcludePattern> patterns = createIncludeExcludeList(new boolean[] { true }, new String[] { ".*" });
		JdkRegExpOrderedIncludeExcludeList matcher = new JdkRegExpOrderedIncludeExcludeList(false, patterns);
		assertTrue("Did not match expected name", matcher.isIncluded("anyoldthing"));

	}

	@Test
	public void testNoPatternsIncludeByDefaultTrue() {
		List<IncludeExcludePattern> patterns = createIncludeExcludeList(new boolean[] {}, new String[] {});
		JdkRegExpOrderedIncludeExcludeList matcher = new JdkRegExpOrderedIncludeExcludeList(true, patterns);
		assertTrue("Did not match expected name", matcher.isIncluded("anyoldthing"));
	}

	@Test
	public void testNoPatternsIncludeByDefaultFalse() {
		List<IncludeExcludePattern> patterns = createIncludeExcludeList(new boolean[] {}, new String[] {});
		JdkRegExpOrderedIncludeExcludeList matcher = new JdkRegExpOrderedIncludeExcludeList(false, patterns);
		assertFalse("Unexpected match when match by default false and no patterns", matcher.isIncluded("anyoldthing"));
	}

	
	@Test
	public void testExcludeThenIncludeWithIncludeByDefaultFalse() {
		List<IncludeExcludePattern> patterns = createIncludeExcludeList(new boolean[] {false, true}, new String[] {"admin.*",".*"});
		JdkRegExpOrderedIncludeExcludeList matcher = new JdkRegExpOrderedIncludeExcludeList(false, patterns);
		assertFalse("Unexpected match when match by default false and should have been excluded", matcher.isIncluded("adminChannel"));
	}
	
	
	List<IncludeExcludePattern> createIncludeExcludeList(boolean[] includeExclude, String[] patterns) {
		assertEquals("flag and patterns arrays must be same length", includeExclude.length, patterns.length);

		List<IncludeExcludePattern> includeExcludePatterns = new ArrayList<IncludeExcludePattern>(patterns.length);
		for (int i = 0; i < includeExclude.length; i++) {
			includeExcludePatterns.add(new IncludeExcludePattern(includeExclude[i], patterns[i]));
		}
		return includeExcludePatterns;
	}

}
