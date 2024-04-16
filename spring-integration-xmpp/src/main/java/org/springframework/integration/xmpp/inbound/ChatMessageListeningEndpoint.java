/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.xmpp.inbound;

import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Stanza;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.xmpp.core.AbstractXmppConnectionAwareEndpoint;
import org.springframework.integration.xmpp.support.DefaultXmppHeaderMapper;
import org.springframework.integration.xmpp.support.XmppHeaderMapper;
import org.springframework.util.Assert;

/**
 * This component logs in as a user and forwards any messages <em>to</em> that
 * user on to downstream components.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Florian Schmaus
 *
 * @since 2.0
 */
public class ChatMessageListeningEndpoint extends AbstractXmppConnectionAwareEndpoint {

	private final StanzaListener stanzaListener = new ChatMessagePublishingStanzaListener();

	private XmppHeaderMapper headerMapper = new DefaultXmppHeaderMapper();

	private Expression payloadExpression;

	private StanzaFilter stanzaFilter;

	private EvaluationContext evaluationContext;

	public ChatMessageListeningEndpoint() {
	}

	public ChatMessageListeningEndpoint(XMPPConnection xmppConnection) {
		super(xmppConnection);
	}

	public void setHeaderMapper(XmppHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	/**
	 * Specify a {@link StanzaFilter} to use for the incoming packets.
	 * @param stanzaFilter the {@link StanzaFilter} to use
	 * @since 4.3
	 * @see XMPPConnection#addAsyncStanzaListener(StanzaListener, StanzaFilter)
	 */
	public void setStanzaFilter(StanzaFilter stanzaFilter) {
		this.stanzaFilter = stanzaFilter;
	}

	/**
	 * Specify a SpEL expression to evaluate a {@code payload} against an incoming
	 * {@link org.jivesoftware.smack.packet.Message}.
	 * @param payloadExpression the {@link Expression} for payload evaluation.
	 * @since 4.3
	 * @see StanzaListener
	 * @see org.jivesoftware.smack.packet.Message
	 */
	public void setPayloadExpression(Expression payloadExpression) {
		this.payloadExpression = payloadExpression;
	}

	@Override
	public String getComponentType() {
		return "xmpp:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	protected void doStart() {
		Assert.isTrue(isInitialized(), () -> getComponentName() + " [" + getComponentType() + "] must be initialized");
		getXmppConnection().addAsyncStanzaListener(this.stanzaListener, this.stanzaFilter);
	}

	@Override
	protected void doStop() {
		XMPPConnection xmppConnection = getXmppConnection();
		if (xmppConnection != null) {
			xmppConnection.removeAsyncStanzaListener(this.stanzaListener);
		}
	}

	private class ChatMessagePublishingStanzaListener implements StanzaListener {

		ChatMessagePublishingStanzaListener() {
		}

		@Override
		public void processStanza(Stanza packet) {
			if (packet instanceof org.jivesoftware.smack.packet.Message xmppMessage) {
				Map<String, ?> mappedHeaders =
						ChatMessageListeningEndpoint.this.headerMapper.toHeadersFromRequest(xmppMessage.asBuilder());

				Object messageBody = xmppMessage.getBody();

				if (ChatMessageListeningEndpoint.this.payloadExpression != null) {
					EvaluationContext evaluationContextToUse = ChatMessageListeningEndpoint.this.evaluationContext;

					List<ExtensionElement> extensions = xmppMessage.getExtensions();
					if (extensions.size() == 1) {
						ExtensionElement extension = extensions.get(0);
						evaluationContextToUse = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
						evaluationContextToUse.setVariable("extension", extension);
					}

					messageBody =
							ChatMessageListeningEndpoint.this.payloadExpression
									.getValue(evaluationContextToUse, xmppMessage);
				}

				if (messageBody != null) {
					sendMessage(getMessageBuilderFactory()
							.withPayload(messageBody)
							.copyHeaders(mappedHeaders).build());
				}
				else if (logger.isInfoEnabled()) {
					if (ChatMessageListeningEndpoint.this.payloadExpression != null) {
						logger.info("The 'payloadExpression' ["
								+ ChatMessageListeningEndpoint.this.payloadExpression.getExpressionString()
								+ "] has been evaluated to 'null'. The XMPP Message [" + xmppMessage + "] is ignored.");
					}
					else {
						logger.info("The XMPP Message [" + xmppMessage + "] with empty body is ignored.");
					}
				}
			}
		}

	}

}
