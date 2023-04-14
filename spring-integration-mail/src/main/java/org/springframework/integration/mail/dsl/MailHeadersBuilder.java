/*
 * Copyright 2014-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mail.dsl;

import java.util.function.Function;

import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.support.MapBuilder;
import org.springframework.messaging.Message;

/**
 * The Mail specific {@link MapBuilder} implementation.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 5.0
 */
public class MailHeadersBuilder extends MapBuilder<MailHeadersBuilder, String, Object> {

	/**
	 * Set the subject.
	 * @param subject the subject.
	 * @return the builder.
	 */
	public MailHeadersBuilder subject(String subject) {
		return put(MailHeaders.SUBJECT, subject);
	}

	/**
	 * Set the expression that will be evaluated to determine th subject.
	 * @param subject the subject expression.
	 * @return the builder.
	 */
	public MailHeadersBuilder subjectExpression(String subject) {
		return putExpression(MailHeaders.SUBJECT, subject);
	}

	/**
	 * Set a function that will be invoked to return the subject based on the message.
	 * @param subject the function.
	 * @param <P> the message payload type.
	 * @return the builder.
	 */
	public <P> MailHeadersBuilder subjectFunction(Function<Message<P>, String> subject) {
		return put(MailHeaders.SUBJECT, new FunctionExpression<>(subject));
	}

	/**
	 * Set the To: addresses.
	 * @param to the addresses.
	 * @return the builder.
	 */
	public MailHeadersBuilder to(String... to) {
		return put(MailHeaders.TO, to);
	}

	/**
	 * Set the expression that will be evaluated to determine the To: addresses.
	 * @param to the expression.
	 * @return the builder.
	 */
	public MailHeadersBuilder toExpression(String to) {
		return putExpression(MailHeaders.TO, to);
	}

	/**
	 * Set a function that will be invoked to determine the To: addresses based on the
	 * message.
	 * @param to the function.
	 * @param <P> the message payload type.
	 * @return the builder.
	 */
	public <P> MailHeadersBuilder toFunction(Function<Message<P>, String[]> to) {
		return put(MailHeaders.TO, new FunctionExpression<>(to));
	}

	/**
	 * Set the cc: addresses.
	 * @param cc the addresses.
	 * @return the builder.
	 */
	public MailHeadersBuilder cc(String... cc) {
		return put(MailHeaders.CC, cc);
	}

	/**
	 * Set the expression that will be evaluated to determine the cc: addresses.
	 * @param cc the expression.
	 * @return the builder.
	 */
	public MailHeadersBuilder ccExpression(String cc) {
		return putExpression(MailHeaders.CC, cc);
	}

	/**
	 * Set a function that will be invoked to determine the cc: addresses based on the
	 * message.
	 * @param cc the function.
	 * @param <P> the message payload type.
	 * @return the builder.
	 */
	public <P> MailHeadersBuilder ccFunction(Function<Message<P>, String[]> cc) {
		return put(MailHeaders.CC, new FunctionExpression<>(cc));
	}

	/**
	 * Set the bcc: addresses.
	 * @param bcc the addresses.
	 * @return the builder.
	 */
	public MailHeadersBuilder bcc(String... bcc) {
		return put(MailHeaders.BCC, bcc);
	}

	/**
	 * Set the expression that will be evaluated to determine the bcc: addresses.
	 * @param bcc the expression.
	 * @return the builder.
	 */
	public MailHeadersBuilder bccExpression(String bcc) {
		return putExpression(MailHeaders.BCC, bcc);
	}

	/**
	 * Set a function that will be invoked to determine the bcc: addresses based on the
	 * message.
	 * @param bcc the function.
	 * @param <P> the message payload type.
	 * @return the builder.
	 */
	public <P> MailHeadersBuilder bccFunction(Function<Message<P>, String[]> bcc) {
		return put(MailHeaders.BCC, new FunctionExpression<>(bcc));
	}

	/**
	 * Set the From: address.
	 * @param from the address.
	 * @return the builder.
	 */
	public MailHeadersBuilder from(String from) {
		return put(MailHeaders.FROM, from);
	}

	/**
	 * Set the expression that will be evaluated to determine the {@code From:} address.
	 * @param from the expression.
	 * @return the builder.
	 */
	public MailHeadersBuilder fromExpression(String from) {
		return putExpression(MailHeaders.FROM, from);
	}

	/**
	 * Set a function that will be invoked to determine the {@code From:} address based on the
	 * message.
	 * @param from the function.
	 * @param <P> the message payload type.
	 * @return the builder.
	 */
	public <P> MailHeadersBuilder fromFunction(Function<Message<P>, String> from) {
		return put(MailHeaders.FROM, new FunctionExpression<>(from));
	}

	/**
	 * Set the ReplyTo: address.
	 * @param replyTo the address.
	 * @return the builder.
	 */
	public MailHeadersBuilder replyTo(String replyTo) {
		return put(MailHeaders.REPLY_TO, replyTo);
	}

	/**
	 * Set the expression that will be evaluated to determine the ReplyTo: address.
	 * @param replyTo the expression.
	 * @return the builder.
	 */
	public MailHeadersBuilder replyToExpression(String replyTo) {
		return putExpression(MailHeaders.REPLY_TO, replyTo);
	}

	/**
	 * Set a function that will be invoked to determine the ReplyTo: address based on the
	 * message.
	 * @param replyTo the function.
	 * @param <P> the message payload type.
	 * @return the builder.
	 */
	public <P> MailHeadersBuilder replyToFunction(Function<Message<P>, String> replyTo) {
		return put(MailHeaders.REPLY_TO, new FunctionExpression<>(replyTo));
	}

	/**
	 * Set a multipart mode to use.
	 * Possible values are 0 through 3.
	 * @param multipartMode header value
	 * @return this
	 * @see org.springframework.mail.javamail.MimeMessageHelper
	 */
	public MailHeadersBuilder multipartMode(int multipartMode) {
		return put(MailHeaders.MULTIPART_MODE, multipartMode);
	}

	/**
	 * Set an expression that is evaluated to determine a multipart mode to use.
	 * Possible values are 0 through 3.
	 * @param multipartMode header value.
	 * @return the builder.
	 * @see org.springframework.mail.javamail.MimeMessageHelper
	 */
	public MailHeadersBuilder multipartModeExpression(String multipartMode) {
		return putExpression(MailHeaders.MULTIPART_MODE, multipartMode);
	}

	/**
	 * Set a function that is invoked to determine a multipart mode to use.
	 * Possible values are 0 through 3.
	 * @param multipartMode header value
	 * @param <P> the message payload type.
	 * @return the builder.
	 * @see org.springframework.mail.javamail.MimeMessageHelper
	 */
	public <P> MailHeadersBuilder multipartModeFunction(Function<Message<P>, Integer> multipartMode) {
		return put(MailHeaders.MULTIPART_MODE, new FunctionExpression<>(multipartMode));
	}

	/**
	 * Set a filename for the attachment.
	 * @param attachmentFilename the file name.
	 * @return the builder.
	 */
	public MailHeadersBuilder attachmentFilename(String attachmentFilename) {
		return put(MailHeaders.ATTACHMENT_FILENAME, attachmentFilename);
	}

	/**
	 * Set an expression that will be evaluated to determine the filename for the attachment.
	 * @param attachmentFilename the expression.
	 * @return the builder.
	 */
	public MailHeadersBuilder attachmentFilenameExpression(String attachmentFilename) {
		return putExpression(MailHeaders.ATTACHMENT_FILENAME, attachmentFilename);
	}

	/**
	 * Set a function that will be invoked to determine the filename for the attachment.
	 * @param attachmentFilename the function.
	 * @param <P> the message payload type.
	 * @return the builder.
	 */
	public <P> MailHeadersBuilder attachmentFilenameFunction(Function<Message<P>, String> attachmentFilename) {
		return put(MailHeaders.ATTACHMENT_FILENAME, new FunctionExpression<>(attachmentFilename));
	}

	/**
	 * Set the content type.
	 * @param contentType the content type.
	 * @return the builder.
	 */
	public MailHeadersBuilder contentType(String contentType) {
		return put(MailHeaders.CONTENT_TYPE, contentType);
	}

	/**
	 * Set an expression that will be evaluated to determine the content type.
	 * @param contentType the expression.
	 * @return the builder.
	 */
	public MailHeadersBuilder contentTypeExpression(String contentType) {
		return putExpression(MailHeaders.CONTENT_TYPE, contentType);
	}

	/**
	 * Set a function that will be invoked to determine the content type.
	 * @param contentType the expression.
	 * @param <P> the message payload type.
	 * @return the builder.
	 */
	public <P> MailHeadersBuilder contentTypeFunction(Function<Message<P>, String> contentType) {
		return put(MailHeaders.CONTENT_TYPE, new FunctionExpression<>(contentType));
	}

	private MailHeadersBuilder putExpression(String key, String expression) {
		return put(key, PARSER.parseExpression(expression));
	}

	MailHeadersBuilder() {
	}

}
