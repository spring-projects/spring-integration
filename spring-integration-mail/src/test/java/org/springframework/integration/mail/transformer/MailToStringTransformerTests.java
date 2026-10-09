/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mail.transformer;

import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;

import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

/**
 * @author Amlan Mishra
 *
 * @since 7.0.7
 */
class MailToStringTransformerTests {

	@Test
	void upstreamHeadersPreservedDuringTransformation() throws Exception {

		jakarta.mail.Message mailMessage = GreenMailUtil.newMimeMessage("");
		mailMessage.setText("test email content");
		mailMessage.setSubject("email-subject");
		mailMessage.setFrom(new InternetAddress("sender@example.com"));

		Message<jakarta.mail.Message> message = MessageBuilder.withPayload(mailMessage)
				.setHeader("customUpstreamHeader", "customValue")
				.setHeader(MailHeaders.SUBJECT, "upstream-subject-to-be-overridden")
				.build();


		MailToStringTransformer transformer = new MailToStringTransformer();

		Message<?> result = transformer.transform(message);

		assertThat(result)
				.returns("test email content", Message::getPayload)
				.extracting(Message::getHeaders, as(MAP))
				.containsEntry("customUpstreamHeader", "customValue")
				.containsEntry(MailHeaders.FROM, "sender@example.com")
				.containsEntry(MailHeaders.SUBJECT, "email-subject");
	}

}
