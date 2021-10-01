package org.rapturemain.tcpmessengerclient;

import org.rapturemain.tcpmessengermessageframework.message.messages.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

@FunctionalInterface
public interface Action {
    Message<?> run(String text);
}
