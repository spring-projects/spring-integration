package org.springframework.integration.atomic;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SpringIntegrationDistributedLatchTests {

private List<Integer> sequence;
	
	@Test
	public void testWithSpringIntegrationDistributedLatch() throws Exception{
		sequence = new LinkedList<Integer>();
		int clients = 5;
		String correlationId = "oleg";
		ExecutorService executor = Executors.newCachedThreadPool();
		JedisConnectionFactory cf = new JedisConnectionFactory();
		cf.afterPropertiesSet();
		
		final SpringIntegrationDistributedLatch latchA = new SpringIntegrationDistributedLatch(0, correlationId);
		final SpringIntegrationDistributedLatch latchB = new SpringIntegrationDistributedLatch(1, correlationId);
		final SpringIntegrationDistributedLatch latchC = new SpringIntegrationDistributedLatch(2, correlationId);
		final SpringIntegrationDistributedLatch latchD = new SpringIntegrationDistributedLatch(3, correlationId);
		final SpringIntegrationDistributedLatch latchE = new SpringIntegrationDistributedLatch(4, correlationId);
		
		for (int i = 0; i < clients; i++) {
			executor.execute(new Runnable() {		
				public void run() {
					processMessage(5, latchE);
				}
			});
		}
		
		for (int i = 0; i < clients; i++) {
			executor.execute(new Runnable() {		
				public void run() {
					processMessage(4, latchD);
				}
			});
		}
		
		for (int i = 0; i < clients; i++) {
			executor.execute(new Runnable() {		
				public void run() {
					processMessage(3, latchC);
				}
			});
		}
		
		for (int i = 0; i < clients; i++) {
			executor.execute(new Runnable() {		
				public void run() {
					processMessage(2, latchB);
				}
			});
		}
		for (int i = 0; i < clients; i++) {
			executor.execute(new Runnable() {		
				public void run() {
					processMessage(1, latchA);
				}
			});
		}
		
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		assertFalse(sequence.isEmpty());
		assertEquals(5, sequence.size());
		assertEquals((Integer)1, sequence.get(0));
		assertEquals((Integer)2, sequence.get(1));
		assertEquals((Integer)3, sequence.get(2));
		assertEquals((Integer)4, sequence.get(3));
		assertEquals((Integer)5, sequence.get(4));
	}
	
	private void processMessage(int messageNumber, SpringIntegrationDistributedLatch distributedLatch){
		try {
			if (distributedLatch.await()){
				System.out.println("Procesing Message: " + messageNumber);
				Thread.sleep(1000);
				System.out.println("Procesed Message: " + messageNumber);
				sequence.add(messageNumber);
				distributedLatch.countDown();
			}
			else {
				System.out.println("Skipping Message " + messageNumber + "  since someone is already processing it");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
