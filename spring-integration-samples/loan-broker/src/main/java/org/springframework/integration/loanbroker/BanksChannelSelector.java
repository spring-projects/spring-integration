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

package org.springframework.integration.loanbroker;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.core.Message;

/**
 * Will select channel names from the map of banks based on provided SpEL expression.<br>
 * For example:<br><pre>
 * headers['CREDIT_SCORE'].score > 780 ? #banks.?[value.equals('premier')].keySet()
 *									   : #banks.?[value.equals('secondary')].keySet()
 * </pre><br>
 * The above SpEL expression upon evaluation will return the list of premier bank 
 * channels if customer's credit score (retrieved from message header 'CREDIT_SCORE') is above 780. 
 * 
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 */
public class BanksChannelSelector {

	private static Logger logger = Logger.getLogger(BanksChannelSelector.class);

	private static final ExpressionParser parser = new SpelExpressionParser();


	private volatile Expression routingExpression;

	private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();


	/**
	 * @param bankChannelPool
	 */
	public BanksChannelSelector(Map<String, String> bankChannelPool) {
		this.evaluationContext.setVariable("banks", bankChannelPool);
	}

	/**
	 * @param routingExpression
	 */
	public void setRoutingExpression(String routingExpression) {
		this.routingExpression = parser.parseExpression(routingExpression);
	}

	/**
	 * @param message
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Set<String> selectBankChannels(Message<?> message) {
		Set<String> bankChannels = (Set<String>) routingExpression.getValue(this.evaluationContext, message);
		logger.debug("selected bank channels: " + bankChannels);
		return bankChannels;
	}

}
