package org.rapturemain.tcpmessengerclient;

import org.rapturemain.tcpmessengermessageframework.message.messages.Message;

@FunctionalInterface
public interface Action {
    Message<?> run(String text);
}
