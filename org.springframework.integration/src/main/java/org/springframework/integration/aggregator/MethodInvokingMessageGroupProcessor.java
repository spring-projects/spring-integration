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

package org.springframework.integration.aggregator;

import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * MessageGroupProcessor that serves as a wrapper around a POJO.
 *
 * @author Iwein Fuld
 * @since 2.0.0
 */
public class MethodInvokingMessageGroupProcessor implements MessageGroupProcessor {


    private final MessageListMethodAdapter adapter;

    /**
     * Creates a wrapper around the target passed in. This constructor will choose the best fitting method and throw an
     * exception when methods are ambiguous or no fitting methods can be found.
     *
     * @param target the object to wrap
     * @throws IllegalStateException when no single method can be found unambiguously
     */
    public MethodInvokingMessageGroupProcessor(Object target) {
        this.adapter = new MessageListMethodAdapter(target, selectMethodFrom(target));
    }

    /**
     * Creates a wrapper around the object passed in. This constructor will look for a named method specifically and
     * fail when it cannot find a method with the given name.
     *
     * @param target the object to wrap
     * @param method the name of the method to look for
     */
    public MethodInvokingMessageGroupProcessor(Object target, String method) {
        this.adapter = new MessageListMethodAdapter(target, method);
    }

    private Method selectMethodFrom(Object target) {
        Method[] methods = target.getClass().getMethods();
        Set<Method> candidates = new HashSet<Method>(Arrays.asList(methods));

        removeObjectMethodsFrom(candidates);
        removeVoidMethodsFrom(candidates);
        removeListIncompatibleMethodsFrom(candidates);
        Set<Method> notAnnotatedCandidates = new HashSet<Method>();
        if (candidates.size() > 1) {
            notAnnotatedCandidates.addAll(removeNotAnnotatedFrom(candidates));
        }
        //if no methods are annotated we need to look in more detail in the unannotated methods
        if (candidates.size() < 1) {
            candidates = notAnnotatedCandidates;
            removeUnfittingFrom(candidates);
        }

        Assert.state(candidates.size() == 1,
                "Method selection failed, there should be exactly one candidate, found ["
                        + candidates + "]");
        return candidates.iterator().next();
    }

    private void removeListIncompatibleMethodsFrom(Set<Method> candidates) {
        removeMethodsMatchingSelector(candidates, new MethodSelector() {
            public boolean select(Method method) {
                int found = 0;
                for (Class<?> parameterClass : method.getParameterTypes()) {
                    if (parameterClass.isAssignableFrom(List.class)) {
                        found++;
                    }
                }
                return found != 1;
            }
        });
    }

    private void removeVoidMethodsFrom(Set<Method> candidates) {
        removeMethodsMatchingSelector(candidates, new MethodSelector() {
            public boolean select(Method method) {
                return method.getReturnType().getName().equals("void");
            }
        });
    }

    private Set<Method> removeNotAnnotatedFrom(Set<Method> candidates) {
        return removeMethodsMatchingSelector(candidates, new MethodSelector() {
            public boolean select(Method method) {
                Aggregator annotation = method.getAnnotation(Aggregator.class);
                return (annotation == null);
            }
        });
    }

    private Set<Method> removeUnfittingFrom(Set<Method> candidates) {
        return removeMethodsMatchingSelector(candidates, new MethodSelector() {
            public boolean select(Method method) {
                Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                Class<?>[] parameterTypes = method.getParameterTypes();
                return (!isFittinglyAnnotated(parameterTypes, parameterAnnotations));
            }
        });
    }

    private boolean isFittinglyAnnotated(Class<?>[] parameterTypes, Annotation[][] parameterAnnotations) {
        int candidateParametersFound = 0;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType.isAssignableFrom(List.class)) {
                boolean headerAnnotationFound = false;
                for (Annotation annotation : parameterAnnotations[i]) {
                    if (annotation instanceof Header) {
                        headerAnnotationFound = true;
                    }
                }
                if (!headerAnnotationFound) {
                    candidateParametersFound++;
                }
            }
        }
        return candidateParametersFound == 1;
    }

    private void removeObjectMethodsFrom(Set<Method> candidates) {
        removeMethodsMatchingSelector(candidates, new MethodSelector() {
            public boolean select(Method method) {
                return method.getDeclaringClass().equals(Object.class);
            }
        });
    }

    public void processAndSend(MessageGroup group,
                               MessageChannelTemplate channelTemplate, MessageChannel outputChannel) {
        final Collection<Message<?>> messagesUpForProcessing = group.getMessages();
        Message reply = MessageBuilder.withPayload(
                this.adapter.executeMethod(messagesUpForProcessing)).build();

        group.onCompletion();
        group.onProcessingOf(messagesUpForProcessing
                .toArray(new Message[messagesUpForProcessing.size()]));

        channelTemplate.send(reply, outputChannel);
    }

    private Set<Method> removeMethodsMatchingSelector(Set<Method> candidates, MethodSelector selector) {
        Set<Method> removed = new HashSet<Method>();
        Iterator<Method> iterator = candidates.iterator();
        while (iterator.hasNext()) {
            Method method = iterator.next();
            if (selector.select(method)) {
                iterator.remove();
                removed.add(method);
            }
        }
        return removed;
    }


    private interface MethodSelector {
        boolean select(Method method);
    }

}
