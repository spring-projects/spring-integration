/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.jdbc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupQueue;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
/**
 * @author Oleg Zhurakousky
 */
public class MessageGroupQueueTests {

	
	@Test
	public void validateMgqInterruption() throws Exception{
		
		final MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), 1, 1);
		
		final AtomicReference<InterruptedException> exceptionHolder = new AtomicReference<InterruptedException>();
		
		Thread t = new Thread(new Runnable() {
			
			public void run() {
				queue.offer(new GenericMessage<String>("hello"));
				try {
					queue.offer(new GenericMessage<String>("hello"), 100, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					exceptionHolder.set(e);
				}
			}
		});
		t.start();
		Thread.sleep(1000);
		t.interrupt();
		Thread.sleep(1000);
		assertTrue(exceptionHolder.get() instanceof InterruptedException);
	}
	
	@Test
	public void testConcurrentReadWrite() throws Exception{
		final MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), 1, 1);
		final AtomicReference<Message<?>> messageHolder = new AtomicReference<Message<?>>();
		
		Thread t1 = new Thread(new Runnable() {		
			public void run() {
				try {
					messageHolder.set(queue.poll(1000, TimeUnit.SECONDS));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {		
			public void run() {
				try {
					queue.offer(new GenericMessage<String>("hello"), 1000, TimeUnit.SECONDS);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t1.start();
		t2.start();
		Thread.sleep(1000);
		assertTrue(messageHolder.get() instanceof Message);
	}
	
	@Test
	public void testConcurrentWriteRead() throws Exception{
		final MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), 1, 1);
		final AtomicReference<Message<?>> messageHolder = new AtomicReference<Message<?>>();
		
		queue.offer(new GenericMessage<String>("hello"), 1000, TimeUnit.SECONDS);
		
		Thread t1 = new Thread(new Runnable() {		
			public void run() {
				try {
					queue.offer(new GenericMessage<String>("Hi"), 1000, TimeUnit.SECONDS);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {		
			public void run() {
				try {
					queue.poll(1000, TimeUnit.SECONDS);
					messageHolder.set(queue.poll(1000, TimeUnit.SECONDS));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		t1.start();
		Thread.sleep(1000);
		t2.start();
		Thread.sleep(1000);
		assertTrue(messageHolder.get().getPayload().equals("Hi"));
	}
	
	@Test
	public void testConcurrentReadersWithTimeout() throws Exception{
		final MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), 1, 1);
		final AtomicReference<Message<?>> messageHolder1 = new AtomicReference<Message<?>>();
		final AtomicReference<Message<?>> messageHolder2 = new AtomicReference<Message<?>>();
		final AtomicReference<Message<?>> messageHolder3 = new AtomicReference<Message<?>>();
		
		Thread t1 = new Thread(new Runnable() {		
			public void run() {
				try {
					messageHolder1.set(queue.poll(10, TimeUnit.SECONDS));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {		
			public void run() {
				try {
					messageHolder2.set(queue.poll(10, TimeUnit.SECONDS));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Thread t3 = new Thread(new Runnable() {		
			public void run() {
				try {
					messageHolder3.set(queue.poll(10, TimeUnit.SECONDS));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Thread t4 = new Thread(new Runnable() {		
			public void run() {
				try {
					queue.offer(new GenericMessage<String>("Hi"), 10, TimeUnit.SECONDS);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t1.start();
		Thread.sleep(1000);
		t2.start();
		Thread.sleep(1000);
		t3.start();
		Thread.sleep(1000);
		t4.start();
		Thread.sleep(1000);
		assertTrue(messageHolder1.get().getPayload().equals("Hi"));
		Thread.sleep(4000);
		assertTrue(messageHolder2.get() == null);
	}
	
	@Test
	public void testConcurrentWritersWithTimeout() throws Exception{
		final MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), 1, 1);
		final AtomicReference<Boolean> booleanHolder1 = new AtomicReference<Boolean>(true);
		final AtomicReference<Boolean> booleanHolder2 = new AtomicReference<Boolean>(true);
		final AtomicReference<Boolean> booleanHolder3 = new AtomicReference<Boolean>(true);
		
		Thread t1 = new Thread(new Runnable() {		
			public void run() {
				try {
					booleanHolder1.set(queue.offer(new GenericMessage<String>("Hi-1"), 2, TimeUnit.SECONDS));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {		
			public void run() {
				try {
					boolean offered = queue.offer(new GenericMessage<String>("Hi-2"), 2, TimeUnit.SECONDS);
					System.out.println(offered);
					booleanHolder2.set(offered);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Thread t3 = new Thread(new Runnable() {		
			public void run() {
				try {
					boolean offered = queue.offer(new GenericMessage<String>("Hi-3"), 2, TimeUnit.SECONDS);
					System.out.println(offered);
					booleanHolder3.set(offered);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t1.start();
		Thread.sleep(1000);
		t2.start();
		Thread.sleep(100);
		t3.start();
		Thread.sleep(4000);
		assertTrue(booleanHolder1.get());
		assertFalse(booleanHolder2.get());
		assertFalse(booleanHolder3.get());
	}
	@Test
    public void testConcurrentWriteReadMulti() throws Exception{
            final MessageGroupQueue queue = new MessageGroupQueue(new SimpleMessageStore(), 1, 4);
            final AtomicReference<Message<?>> messageHolder = new AtomicReference<Message<?>>();
            
            queue.offer(new GenericMessage<String>("hello"), 1000, TimeUnit.SECONDS);
            
            Thread t1 = new Thread(new Runnable() {                
                    public void run() {
                            try {
                                    queue.offer(new GenericMessage<String>("Hi"), 1000, TimeUnit.SECONDS);
                                    queue.offer(new GenericMessage<String>("Hi"), 1000, TimeUnit.SECONDS);
                                    queue.offer(new GenericMessage<String>("Hi"), 1000, TimeUnit.SECONDS);
                            } catch (Exception e) {
                                    e.printStackTrace();
                            }
                    }
            });
            Thread t2 = new Thread(new Runnable() {                
                    public void run() {
                            try {
                                    queue.poll(1000, TimeUnit.SECONDS);
                                    messageHolder.set(queue.poll(1000, TimeUnit.SECONDS));
                                    queue.poll(1000, TimeUnit.SECONDS);
                                    queue.poll(1000, TimeUnit.SECONDS);
                            } catch (Exception e) {
                                    e.printStackTrace();
                            }
                    }
            });
            
            t1.start();
            Thread.sleep(1000);
            t2.start();
            Thread.sleep(1000);
            assertTrue(messageHolder.get().getPayload().equals("Hi"));
            assertNull(queue.poll(5, TimeUnit.SECONDS));
    }
	
	@Test
    public void validateMgqInterruptionStoreLock() throws Exception{
            
            MessageGroupStore mgs = Mockito.mock(MessageGroupStore.class);
            Mockito.doAnswer(new Answer<MessageGroup>() {
                    public MessageGroup answer(InvocationOnMock invocation)
                                    throws Throwable {
                            Thread.sleep(5000);
                            return null;
                    }
            }).when(mgs).addMessageToGroup(Mockito.any(Integer.class), Mockito.any(Message.class));
            
            MessageGroup mg = Mockito.mock(MessageGroup.class);
            Mockito.when(mgs.getMessageGroup(Mockito.any())).thenReturn(mg);
            Mockito.when(mg.size()).thenReturn(0);
            
            final MessageGroupQueue queue = new MessageGroupQueue(mgs, 1, 1);
            
            final AtomicReference<InterruptedException> exceptionHolder = new AtomicReference<InterruptedException>();
            
            Thread t1 = new Thread(new Runnable() {
                    
                    public void run() {
                            queue.offer(new GenericMessage<String>("hello"));
                    }
            });
            t1.start();
            Thread.sleep(500);
            Thread t2 = new Thread(new Runnable() {
                    
                    public void run() {
                            queue.offer(new GenericMessage<String>("hello"));
                            try {
                                    queue.offer(new GenericMessage<String>("hello"), 100, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                    exceptionHolder.set(e);
                            }
                    }
            });
            t2.start();
            Thread.sleep(1000);
            t2.interrupt();
            Thread.sleep(1000);
            assertTrue(exceptionHolder.get() instanceof InterruptedException);
    }
}
