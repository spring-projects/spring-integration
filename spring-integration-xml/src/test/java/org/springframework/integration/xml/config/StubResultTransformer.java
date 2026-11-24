/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.xml.config;

import javax.xml.transform.Result;

import org.springframework.integration.xml.transformer.ResultTransformer;

public class StubResultTransformer implements ResultTransformer {

	Object toReturn;

	public StubResultTransformer(Object toReturn) {
		this.toReturn = toReturn;
	}

	public Object transformResult(Result res) {
		return toReturn;
	}

}
