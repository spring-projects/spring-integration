/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.xml.result;

import javax.xml.transform.Result;

import org.springframework.xml.transform.StringResult;

/**
 * @author Jonas Partner
 */
public class StringResultFactory implements ResultFactory {

	public Result createResult(Object payload) {
		return new StringResult();
	}

}
