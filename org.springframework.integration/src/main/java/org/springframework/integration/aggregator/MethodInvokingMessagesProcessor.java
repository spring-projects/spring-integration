package org.springframework.integration.aggregator;

import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class MethodInvokingMessagesProcessor implements MessagesProcessor {

    private final Object target;
    private final Method method;

    private final MessageListMethodAdapter adapter;

    public MethodInvokingMessagesProcessor(Object target) {
        this.target = target;
        this.method = selectMethodFrom(target);
        this.adapter = new MessageListMethodAdapter(target, method);
    }

    private Method selectMethodFrom(Object target) {
        Method[] methods = target.getClass().getMethods();
        Set<Method> candidates = new HashSet<Method>(Arrays.asList(methods));

        removeObjectMethodsFrom(candidates);
        removeVoidMethodsFrom(candidates);
        removeListIncompatibleMethodsFrom(candidates);
        Set<Method> unanotatedCandidates = new HashSet<Method>();
        if (candidates.size() > 1) {
            unanotatedCandidates.addAll(removeUnanotatedFrom(candidates));
        }
        //if no methods are annotated we need to look in more detail in the unannotated methods
        if (candidates.size() < 1) {
            candidates = unanotatedCandidates;
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

    private Set<Method> removeUnanotatedFrom(Set<Method> candidates) {
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

    public void processAndSend(Object correlationKey,
                               Collection<Message<?>> messagesUpForProcessing,
                               MessageChannel outputChannel,
                               BufferedMessagesCallback processedCallback) {
        Message reply = MessageBuilder.withPayload(
                this.adapter.executeMethod(messagesUpForProcessing)).build();
        processedCallback.onCompletionOf(correlationKey);
        processedCallback.onProcessingOf(messagesUpForProcessing
                .toArray(new Message[]{}));
        outputChannel.send(reply);
    }

    public Set<Method> removeMethodsMatchingSelector(Set<Method> candidates, MethodSelector selector) {
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
