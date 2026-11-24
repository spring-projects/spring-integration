/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.xml.config;

import javax.xml.transform.Result;

import org.springframework.integration.xml.result.ResultFactory;
import org.springframework.xml.transform.StringResult;

public class StubResultFactory implements ResultFactory {

	public Result createResult(Object payload) {
		return new StubStringResult();
	}

	public static class StubStringResult extends StringResult {

	}

}
