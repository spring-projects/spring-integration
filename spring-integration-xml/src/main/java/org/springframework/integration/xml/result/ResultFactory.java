/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.xml.result;

import javax.xml.transform.Result;

/**
 * Factory to create a {@link Result} possibly taking into account the
 * provided message payload instance.
 *
 * @author Jonas Partner
 */
public interface ResultFactory {

	Result createResult(Object payload);

}
