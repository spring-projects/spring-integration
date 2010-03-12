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
package org.springframework.integration.loanbroker.domain;

import java.text.DecimalFormat;
import java.util.Date;

/**
 * @author Oleg Zhurakousky
 *
 */
public class LoanQuote implements Comparable<LoanQuote>{

	private String lender;
	private Date quoteDate;
	private Date expirationDate;
	private double loanAmount;
	private int loanTerm;
	private float rate;
	
	public String getLender() {
		return lender;
	}
	public void setLender(String lender) {
		this.lender = lender;
	}
	public Date getQuoteDate() {
		return quoteDate;
	}
	public void setQuoteDate(Date quoteDate) {
		this.quoteDate = quoteDate;
	}
	public Date getExpirationDate() {
		return expirationDate;
	}
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}
	public double getLoanAmount() {
		return loanAmount;
	}
	public void setLoanAmount(double loanAmount) {
		this.loanAmount = loanAmount;
	}
	public int getLoanTerm() {
		return loanTerm;
	}
	public void setLoanTerm(int loanTerm) {
		this.loanTerm = loanTerm;
	}
	public float getRate() {
		return rate;
	}
	public void setRate(float rate) {
		this.rate = rate;
	}

	public String toString(){
		return "\n====== Loan Quote =====\n" +
			   "Lender: " + lender + "\n" +
			   "Loan amount: " + new DecimalFormat("$###,###.###").format(loanAmount) + "\n" + 
			   "Quotation Date: " + quoteDate + "\n" +
			   "Expiration Date: " + expirationDate + "\n" + 
			   "Term: " + loanTerm + " years" + "\n" +
			   "Rate: " + rate + "%\n" + 
			   "=======================\n";
			   
	}

	public int compareTo(LoanQuote loanQuote) {
		if (loanQuote.rate > this.rate){
			return 1;
		} else if (loanQuote.rate < this.rate){
			return -1;
		}
		return 0;
	}
}
