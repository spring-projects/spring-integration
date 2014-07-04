/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.http.inbound;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.MessageSource;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.Controller;

/**
 * Inbound HTTP endpoint that implements Spring's {@link Controller} interface to be used with a DispatcherServlet front
 * controller.
 * <p>
 * The {@link #setViewName(String) viewName} will be passed into the ModelAndView return value.
 * <p>
 * This endpoint will have request/reply behavior by default. That can be overridden by passing <code>false</code> to
 * the constructor. In the request/reply case, the core map will be passed to the view, and it will contain either the
 * reply Message or payload depending on the value of {@link #extractReplyPayload} (true by default, meaning just the
 * payload). The corresponding key in the map is determined by the {@link #replyKey} property (with a default of
 * "reply").
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class HttpRequestHandlingController extends HttpRequestHandlingEndpointSupport implements Controller {

	private static final String DEFAULT_ERROR_CODE = "spring.integration.http.handler.error";

	private static final String DEFAULT_REPLY_KEY = "reply";

	private static final String DEFAULT_ERRORS_KEY = "errors";

	private volatile Expression viewExpression;

	private volatile StandardEvaluationContext evaluationContext;

	private volatile String replyKey = DEFAULT_REPLY_KEY;

	private volatile String errorsKey = DEFAULT_ERRORS_KEY;

	private volatile String errorCode = DEFAULT_ERROR_CODE;

	public HttpRequestHandlingController() {
		this(true);
	}

	public HttpRequestHandlingController(boolean expectReply) {
		super(expectReply);
	}

	/**
	 * Specify the view name.
	 *
	 * @param viewName The view name.
	 */
	public void setViewName(String viewName) {
		Assert.isTrue(StringUtils.hasText(viewName), "View name must contain text");
		this.viewExpression = new LiteralExpression(viewName);
	}

	/**
	 * Specify the key to be used when adding the reply Message or payload to the core map (will be payload only unless
	 * the value of {@link HttpRequestHandlingController#setExtractReplyPayload(boolean)} is <code>false</code>). The
	 * default key is "reply".
	 *
	 * @param replyKey The reply key.
	 */
	public void setReplyKey(String replyKey) {
		this.replyKey = (replyKey != null) ? replyKey : DEFAULT_REPLY_KEY;
	}

	/**
	 * The key used to expose {@link Errors} in the core, in the case that message handling fails. Defaults to
	 * "errors".
	 *
	 * @param errorsKey The key value to set.
	 */
	public void setErrorsKey(String errorsKey) {
		this.errorsKey = errorsKey;
	}

	/**
	 * The error code to use to signal an error in the message handling. In the case of an error this code will be
	 * provided in an object error to be optionally translated in the standard MVC way using a {@link MessageSource}.
	 * The default value is <code>spring.integration.http.handler.error</code>. Three arguments are provided: the
	 * exception, its message and its stack trace as a String.
	 *
	 * @param errorCode The error code to set.
	 */
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * Specifies a SpEL expression to evaluate in order to generate the view name.
	 * The EvaluationContext will be populated with the reply message as the root object,
	 *
	 * @param viewExpression The view expression.
	 */
	public void setViewExpression(Expression viewExpression) {
		this.viewExpression = viewExpression;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		this.evaluationContext = this.createEvaluationContext();
	}

	/**
	 * Handles the HTTP request by generating a Message and sending it to the request channel. If this gateway's
	 * 'expectReply' property is true, it will also generate a response from the reply Message once received.
	 */
	@Override
	public final ModelAndView handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws Exception {
		ModelAndView modelAndView = new ModelAndView();
		try {
			Message<?> replyMessage = super.doHandleRequest(servletRequest, servletResponse);
			ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
			if (replyMessage != null) {
				Object reply = setupResponseAndConvertReply(response, replyMessage);
				response.close();
				modelAndView.addObject(this.replyKey, reply);
			}
			else {
				setStatusCodeIfNeeded(response);
			}

			if (this.viewExpression != null) {
				Object view;
				if (replyMessage != null) {
					view = this.viewExpression.getValue(this.evaluationContext, replyMessage);
				}
				else {
					view = this.viewExpression.getValue(this.evaluationContext);
				}
				if (view instanceof View) {
					modelAndView.setView((View) view);
				}
				else if (view instanceof String) {
					modelAndView.setViewName((String) view);
				}
				else {
					throw new IllegalStateException("view expression must resolve to a View or String");
				}
			}
		}
		catch (Exception e) {
			MapBindingResult errors = new MapBindingResult(new HashMap<String, Object>(), "dummy");
			PrintWriter stackTrace = new PrintWriter(new StringWriter());
			e.printStackTrace(stackTrace);
			errors.reject(errorCode, new Object[] { e, e.getMessage(), stackTrace.toString() },
					"A Spring Integration handler raised an exception while handling an HTTP request.  The exception is of type "
							+ e.getClass() + " and it has a message: (" + e.getMessage() + ")");
			modelAndView.addObject(errorsKey, errors);
		}
		return modelAndView;
	}

}
