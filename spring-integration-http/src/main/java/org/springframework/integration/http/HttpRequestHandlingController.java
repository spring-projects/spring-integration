/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class HttpRequestHandlingController extends HttpRequestHandlingEndpointSupport implements Controller {

	private static final String DEFAULT_REPLY_KEY = "reply";


	private volatile String viewName;

	private volatile String replyKey = DEFAULT_REPLY_KEY;


	public HttpRequestHandlingController() {
		this(true);
	}

	public HttpRequestHandlingController(boolean expectReply) {
		super(expectReply);
	}


	/**
	 * Specify the view name.
	 */
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	/**
	 * Specify the key to be used when adding the reply Message or payload
	 * to the model map (will be payload only unless the value of
	 * {@link #setExtractReplyPayload(boolean) extractReplyPayload} is <code>false</code>).
	 * The default key is "reply".
	 */
	public void setReplyKey(String replyKey) {
		this.replyKey = (replyKey != null) ? replyKey : DEFAULT_REPLY_KEY;
	}

	/**
	 * Handles the HTTP request by generating a Message and sending it to the request channel.
	 * If this gateway's 'expectReply' property is true, it will also generate a response from
	 * the reply Message once received.
	 */
	public final ModelAndView handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
		ModelAndView modelAndView = new ModelAndView();
		if (this.viewName != null) {
			modelAndView.setViewName(this.viewName);
		}
		Object reply = super.doHandleRequest(servletRequest, servletResponse);
		if (reply != null) {
			modelAndView.addObject(this.replyKey, reply);
		}
		return modelAndView;
	}

}
