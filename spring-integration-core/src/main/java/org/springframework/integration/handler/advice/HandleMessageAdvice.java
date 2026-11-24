/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.handler.advice;

import org.aopalliance.intercept.MethodInterceptor;

/**
 * The marker {@link MethodInterceptor} interface extension
 * to distinguish advices for some reason.
 *
 * @author Artem Bilan
 * @since 4.3.1
 */
public interface HandleMessageAdvice extends MethodInterceptor {

}
