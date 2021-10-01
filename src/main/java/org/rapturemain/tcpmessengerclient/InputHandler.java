package org.rapturemain.tcpmessengerclient;

import lombok.Getter;
import lombok.Setter;
import org.rapturemain.tcpmessengermessageframework.message.MessageEncoderDecoder;
import org.rapturemain.tcpmessengermessageframework.message.messages.Message;

import java.io.*;
import java.util.function.Consumer;

public class InputHandler extends Thread {

    private final MessageEncoderDecoder encoderDecoder;
    private final DataInputStream dataInputStream;
    private final DataOutputStream dataOutputStream;
    private final MessageHandler messageHandler;
    private final BufferedWriter outputStream;

    @Getter @Setter
    private Consumer<Exception> onEOF = (e) -> {};
    @Getter @Setter
    private Consumer<Exception> onIOE = (e) -> {};

    private volatile boolean disabled = false;


    public InputHandler(MessageEncoderDecoder messageEncoderDecoder, DataInputStream dataInputStream, DataOutputStream dataOutputStream, BufferedWriter outputStream) {
        this.encoderDecoder = messageEncoderDecoder;
        this.messageHandler = new MessageHandler(messageEncoderDecoder);
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;
        this.outputStream = outputStream;
    }

    public void disable() {
        this.disabled = true;
    }

    @Override
    public void run() {
        try {
            Message<?> message;

            byte[] buf = new byte[0];
            while (!disabled) {
                try {
                    int res = dataInputStream.read(buf, 0, 0);
                    if (res == -1) {
                        throw new EOFException();
                    }
                } catch (InterruptedIOException e) {
                    continue;
                }

                while ((message = encoderDecoder.decode(dataInputStream)) != null) {
                    messageHandler.handleMessage(message, dataOutputStream, outputStream);
                }
            }
        } catch (EOFException e) {
            onEOF.accept(e);
        } catch (IOException e) {
            onIOE.accept(e);
        }
    }
}
