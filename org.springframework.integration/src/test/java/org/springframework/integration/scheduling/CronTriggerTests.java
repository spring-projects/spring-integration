/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.scheduling;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Before;
import org.junit.Test;

import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

/**
 * @author Dave Syer
 * @author Mark Fisher
 */
public class CronTriggerTests {

	private Calendar calendar = new GregorianCalendar();

	private Date date = new Date();


	/**
	 * @param calendar
	 */
	private void roundup(Calendar calendar) {
		calendar.add(Calendar.SECOND, 1);
		calendar.set(Calendar.MILLISECOND, 0);
	}


	@Before
	public void setUp() {
		calendar.setTime(date);
		roundup(calendar);
	}

	@Test
	public void testMatchAll() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * *");
		SimpleTriggerContext triggerContext = new SimpleTriggerContext();
		triggerContext.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext));
	}

	@Test
	public void testMatchLastSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * *");
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.SECOND, 58);
		assertMatchesNextSecond(trigger, calendar);
	}

	@Test
	public void testMatchSpecificSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("10 * * * * *");
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.SECOND, 9);
		assertMatchesNextSecond(trigger, calendar);
	}

	@Test
	public void testIncrementSecondByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("11 * * * * *");
		calendar.set(Calendar.SECOND, 10);
		Date date = calendar.getTime();
		calendar.add(Calendar.SECOND, 1);
		SimpleTriggerContext triggerContext = new SimpleTriggerContext();
		triggerContext.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext));
	}

	@Test
	public void testIncrementSecondAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("10 * * * * *");
		calendar.set(Calendar.SECOND, 11);
		Date date = calendar.getTime();
		calendar.add(Calendar.SECOND, 59);
		SimpleTriggerContext triggerContext = new SimpleTriggerContext();
		triggerContext.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext));
	}

	@Test
	public void testSecondRange() throws Exception {
		CronTrigger trigger = new CronTrigger("10-15 * * * * *");
		calendar.set(Calendar.SECOND, 9);
		assertMatchesNextSecond(trigger, calendar);
		calendar.set(Calendar.SECOND, 14);
		assertMatchesNextSecond(trigger, calendar);
	}

	@Test
	public void testIncrementMinuteByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("0 11 * * * *");
		calendar.set(Calendar.MINUTE, 10);
		Date date = calendar.getTime();
		calendar.add(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 0);
		SimpleTriggerContext triggerContext = new SimpleTriggerContext();
		triggerContext.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext));
	}

	@Test
	public void testIncrementMinute() throws Exception {
		CronTrigger trigger = new CronTrigger("0 * * * * *");
		calendar.set(Calendar.MINUTE, 10);
		Date date = calendar.getTime();
		calendar.add(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 0);
		SimpleTriggerContext triggerContext1 = new SimpleTriggerContext();
		triggerContext1.update(null, null, date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(triggerContext1));
		calendar.add(Calendar.MINUTE, 1);
		SimpleTriggerContext triggerContext2 = new SimpleTriggerContext();
		triggerContext2.update(null, null, date);
		assertEquals(calendar.getTime(), date=trigger.nextExecutionTime(triggerContext2));
	}

	@Test
	public void testIncrementMinuteAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("0 10 * * * *");
		calendar.set(Calendar.MINUTE, 11);
		calendar.set(Calendar.SECOND, 0);
		Date date = calendar.getTime();
		calendar.add(Calendar.MINUTE, 59);
		SimpleTriggerContext triggerContext = new SimpleTriggerContext();
		triggerContext.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext));
	}

	@Test
	public void testIncrementHour() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 * * * *");
		calendar.set(Calendar.MONTH, 9);
		calendar.set(Calendar.DAY_OF_MONTH, 30);
		calendar.set(Calendar.HOUR_OF_DAY, 11);
		calendar.set(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 0);
		Date date = calendar.getTime();
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.HOUR_OF_DAY, 12);
		SimpleTriggerContext triggerContext1 = new SimpleTriggerContext();
		triggerContext1.update(null, null, date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(triggerContext1));
		calendar.set(Calendar.HOUR_OF_DAY, 13);
		SimpleTriggerContext triggerContext2 = new SimpleTriggerContext();
		triggerContext2.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext2));
	}

	@Test
	public void testIncrementDayOfMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *");
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		Date date = calendar.getTime();
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		SimpleTriggerContext triggerContext1 = new SimpleTriggerContext();
		triggerContext1.update(null, null, date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(triggerContext1));
		assertEquals(2, calendar.get(Calendar.DAY_OF_MONTH));
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		SimpleTriggerContext triggerContext2 = new SimpleTriggerContext();
		triggerContext2.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext2));
		assertEquals(3, calendar.get(Calendar.DAY_OF_MONTH));
	}

	@Test
	public void testIncrementDayOfMonthByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * 10 * *");
		calendar.set(Calendar.DAY_OF_MONTH, 9);
		Date date = calendar.getTime();
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		SimpleTriggerContext triggerContext = new SimpleTriggerContext();
		triggerContext.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext));
	}

	@Test
	public void testIncrementDayOfMonthAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * 10 * *");
		calendar.set(Calendar.DAY_OF_MONTH, 11);
		Date date = calendar.getTime();
		calendar.add(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 10);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		SimpleTriggerContext triggerContext = new SimpleTriggerContext();
		triggerContext.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext));
	}

	@Test
	public void testDailyTriggerInShortMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *");
		calendar.set(Calendar.MONTH, 8); // September: 30 days
		calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = calendar.getTime();
		calendar.set(Calendar.MONTH, 9); // October
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		SimpleTriggerContext triggerContext1 = new SimpleTriggerContext();
		triggerContext1.update(null, null, date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(triggerContext1));
		calendar.set(Calendar.DAY_OF_MONTH, 2);
		SimpleTriggerContext triggerContext2 = new SimpleTriggerContext();
		triggerContext2.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext2));
	}

	@Test
	public void testDailyTriggerInLongMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *");
		calendar.set(Calendar.MONTH, 9); // October: 31 days
		calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = calendar.getTime();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 31);
		SimpleTriggerContext triggerContext1 = new SimpleTriggerContext();
		triggerContext1.update(null, null, date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(triggerContext1));
		calendar.set(Calendar.MONTH, 10); // November
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		SimpleTriggerContext triggerContext2 = new SimpleTriggerContext();
		triggerContext2.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext2));
	}

	@Test
	public void testIncrementMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 1 * *");
		calendar.set(Calendar.MONTH, 9);
		calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = calendar.getTime();
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MONTH, 10);
		SimpleTriggerContext triggerContext1 = new SimpleTriggerContext();
		triggerContext1.update(null, null, date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(triggerContext1));
		calendar.set(Calendar.MONTH, 11);
		SimpleTriggerContext triggerContext2 = new SimpleTriggerContext();
		triggerContext2.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext2));
	}

	@Test
	public void testMonthlyTriggerInLongMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 31 * *");
		calendar.set(Calendar.MONTH, 9);
		calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = calendar.getTime();
		calendar.set(Calendar.DAY_OF_MONTH, 31);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		SimpleTriggerContext triggerContext = new SimpleTriggerContext();
		triggerContext.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext));
	}

	@Test
	public void testMonthlyTriggerInShortMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 1 * *");
		calendar.set(Calendar.MONTH, 9);
		calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = calendar.getTime();
		calendar.set(Calendar.MONTH, 10);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		SimpleTriggerContext triggerContext = new SimpleTriggerContext();
		triggerContext.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext));
	}

	@Test
	public void testIncrementDayOfWeekByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * 2");
		calendar.set(Calendar.DAY_OF_WEEK, 2);
		Date date = calendar.getTime();
		calendar.add(Calendar.DAY_OF_WEEK, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		SimpleTriggerContext triggerContext = new SimpleTriggerContext();
		triggerContext.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext));
		assertEquals(Calendar.TUESDAY, calendar.get(Calendar.DAY_OF_WEEK));
	}

	@Test
	public void testIncrementDayOfWeekAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * 2");
		calendar.set(Calendar.DAY_OF_WEEK, 4);
		Date date = calendar.getTime();
		calendar.add(Calendar.DAY_OF_MONTH, 6);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		SimpleTriggerContext triggerContext = new SimpleTriggerContext();
		triggerContext.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext));
		assertEquals(Calendar.TUESDAY, calendar.get(Calendar.DAY_OF_WEEK));
	}

	@Test
	public void testDayOfWeekIndifferent() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * 2 * *");
		CronTrigger trigger2 = new CronTrigger("* * * 2 * ?");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSecondIncrementer() throws Exception {
		CronTrigger trigger1 = new CronTrigger("57,59 * * * * *");
		CronTrigger trigger2 = new CronTrigger("57/2 * * * * *");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSecondIncrementerWithRange() throws Exception {
		CronTrigger trigger1 = new CronTrigger("1,3,5 * * * * *");
		CronTrigger trigger2 = new CronTrigger("1-6/2 * * * * *");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testHourIncrementer() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * 4,8,12,16,20 * * *");
		CronTrigger trigger2 = new CronTrigger("* * 4/4 * * *");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testDayNames() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0-6");
		CronTrigger trigger2 = new CronTrigger("* * * * * TUE,WED,THU,FRI,SAT,SUN,MON");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSundayIsZero() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0");
		CronTrigger trigger2 = new CronTrigger("* * * * * SUN");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSundaySynonym() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0");
		CronTrigger trigger2 = new CronTrigger("* * * * * 7");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testMonthNames() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * 0-11 *");
		CronTrigger trigger2 = new CronTrigger("* * * * FEB,JAN,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC *");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testMonthNamesMixedCase() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * 1 *");
		CronTrigger trigger2 = new CronTrigger("* * * * Feb *");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testWhitespace() throws Exception {
		CronTrigger trigger1 = new CronTrigger("*  *  * *  1 *");
		CronTrigger trigger2 = new CronTrigger("* * * * 1 *");
		assertEquals(trigger1, trigger2);
	}


	/**
	 * @param trigger
	 * @param calendar
	 */
	private void assertMatchesNextSecond(CronTrigger trigger, Calendar calendar) {
		Date date = calendar.getTime();
		roundup(calendar);
		SimpleTriggerContext triggerContext = new SimpleTriggerContext();
		triggerContext.update(null, null, date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(triggerContext));
	}

}
