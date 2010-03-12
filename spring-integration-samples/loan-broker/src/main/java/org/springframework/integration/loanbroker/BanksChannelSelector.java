/*
 * Copyright 2002-2008 the original author or authors.
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
import org.springframework.expression.EvaluationContext;
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
 * channels if customer's credit score is above 780. 
 * 
 * @author Oleg Zhurakousky
 *
 */
public class BanksChannelSelector {
	private static Logger logger = Logger.getLogger(BanksChannelSelector.class);
	private Map<String, String> bankChannelPool;
	private String routingExpression;
	/**
	 * 
	 * @param bankChannelPool
	 */
	public BanksChannelSelector(Map<String, String> bankChannelPool){
		this.bankChannelPool = bankChannelPool;
	}
	/**
	 * 
	 * @param message
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Set<String> selectBankChannels(Message<?> message) {
		EvaluationContext context = new StandardEvaluationContext(message);
		
		context.setVariable("banks", bankChannelPool);
		ExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression(routingExpression);
		Set<String> bankChannels = (Set<String>) expression.getValue(context);
		logger.debug("Selected bank channels: " + bankChannels);
		return bankChannels;
	}
	/**
	 * @param routingExpression
	 */
	public void setRoutingExpression(String routingExpression) {
		this.routingExpression = routingExpression;
	}
}
