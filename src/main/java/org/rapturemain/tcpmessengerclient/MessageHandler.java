package org.rapturemain.tcpmessengerclient;

import lombok.Getter;
import lombok.Setter;
import org.rapturemain.tcpmessengermessageframework.message.MessageEncoderDecoder;
import org.rapturemain.tcpmessengermessageframework.message.base.TimestampEntry;
import org.rapturemain.tcpmessengermessageframework.message.messages.*;
import org.rapturemain.tcpmessengermessageframework.message.messages.request.PingRequest;
import org.rapturemain.tcpmessengermessageframework.message.messages.response.PingResponse;
import org.rapturemain.tcpmessengermessageframework.message.messages.response.RegistrationResponseMessage;
import org.rapturemain.tcpmessengermessageframework.message.messages.system.UserConnectedMessage;
import org.rapturemain.tcpmessengermessageframework.message.messages.system.UserDisconnectedMessage;
import org.rapturemain.tcpmessengermessageframework.message.messages.system.UserRegisteredMessage;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class MessageHandler {

    private final MessageEncoderDecoder messageEncoderDecoder;

    @Getter @Setter
    private Runnable onConnectionReset = () -> {};

    public MessageHandler(MessageEncoderDecoder messageEncoderDecoder) {
        this.messageEncoderDecoder = messageEncoderDecoder;
    }

    public void handleMessage(Message<?> message, DataOutputStream socketDOS, BufferedWriter bufferedWriter) throws IOException {
        if (message instanceof ChatMessage<?>) {
            handleChatMessage((ChatMessage<?>) message, bufferedWriter);
        }
        if (message instanceof SystemMessage<?>) {
            handleSystemMessage((SystemMessage<?>) message, socketDOS, bufferedWriter);
        }
        bufferedWriter.flush();
    }

    private void handleChatMessage(ChatMessage<?> message, BufferedWriter writer) throws IOException {
        if (message instanceof SimpleChatMessage) {
            SimpleChatMessage m = (SimpleChatMessage) message;
            writer.write(String.format("<%s> [%s]: %s",
                    getTime(m.getTimestamp()),
                    m.getSenderName().getString(),
                    m.getText().getString()
            ));
            writer.newLine();
            return;
        }
        if (message instanceof FileChatMessage) {
            FileChatMessage m = (FileChatMessage) message;

            String fileName = UUID.randomUUID().toString().substring(0, 10) + "_" + m.getFileName().getString();
            writer.write(String.format("<%s> [%s] {Sent file. Original name: [%s], saved name: [%s]}",
                    getTime(m.getTimestamp()),
                    m.getSenderName().getString(),
                    m.getFileName().getString(),
                    fileName
            ));
            writer.newLine();

            FileOutputStream fileOutputStream = new FileOutputStream(((FileChatMessage) message).getFileName().getString() + UUID.randomUUID().toString().substring(0, 10));
            byte[] bytes = ((FileChatMessage) message).getBytes().getBytes();
            fileOutputStream.write(bytes);
            fileOutputStream.flush();
            fileOutputStream.close();

            writer.write(String.format("<File saved: %s>", fileName));
            writer.newLine();
        }
    }

    private void handleSystemMessage(SystemMessage<?> message, DataOutputStream dos, BufferedWriter writer) throws IOException {
        if (message instanceof ConnectionResetMessage) {
            writer.write("<System Message>: Connection reset by server");
            writer.newLine();
            onConnectionReset.run();
            return;
        }
        if (message instanceof PingRequest) {
            messageEncoderDecoder.encode(new PingResponse(), dos);
            return;
        }
        if (message instanceof RegistrationResponseMessage) {
            RegistrationResponseMessage m = (RegistrationResponseMessage) message;
            if (!m.getSuccess().getValue()) {
                writer.write(String.format("<System Message>: Registration failed. Reason: %s", m.getMessage().getString()));
            } else {
                writer.write(String.format("<System Message>: Registration successful. Name: %s", m.getName().getString()));
            }
            writer.newLine();
            return;
        }
        if (message instanceof UserConnectedMessage) {
            UserConnectedMessage m = (UserConnectedMessage) message;
            writer.write(String.format("<%s> <System Message>: User connected",
                    getTime(m.getTimestamp())
            ));
            writer.newLine();
            return;
        }
        if (message instanceof UserDisconnectedMessage) {
            UserDisconnectedMessage m = (UserDisconnectedMessage) message;
            writer.write(String.format("<%s> <System Message>: User disconnected",
                    getTime(m.getTimestamp())
            ));
            if (m.getName() != null && m.getName().getString() != null) {
                writer.write(". Name: " + m.getName().getString());
            }
            writer.newLine();
        }

        if (message instanceof UserRegisteredMessage) {
            UserRegisteredMessage m = (UserRegisteredMessage) message;
            writer.write(String.format("<%s> <System Message>: User registered. Name: %s",
                    getTime(m.getTimestamp()),
                    m.getName().getString()
            ));
            writer.newLine();
        }
    }

    private String getTime(TimestampEntry timestampEntry) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestampEntry.getTimestamp()),
                ZoneId.systemDefault()
        ).toString();
    }
}
