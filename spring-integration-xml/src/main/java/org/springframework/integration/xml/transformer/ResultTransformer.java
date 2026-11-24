/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.xml.transformer;

import javax.xml.transform.Result;

/**
 * Implementations of this class allow for the transformation of {@link Result}
 * objects to other formats.
 *
 * @author Jonas Partner
 * @author Gunnar Hillert
 *
 */
public interface ResultTransformer {

	Object transformResult(Result result);

}
