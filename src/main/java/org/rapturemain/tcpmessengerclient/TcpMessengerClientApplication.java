package org.rapturemain.tcpmessengerclient;


import org.rapturemain.tcpmessengermessageframework.message.MessageEncoderDecoderImpl;
import org.rapturemain.tcpmessengermessageframework.message.messages.*;
import org.rapturemain.tcpmessengermessageframework.message.messages.request.PingRequest;
import org.rapturemain.tcpmessengermessageframework.message.messages.response.PingResponse;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class TcpMessengerClientApplication {

    public static void main(String[] args) {
        InputStringDecoder decoder = new InputStringDecoder();
        final MessageEncoderDecoderImpl encoderDecoder = new MessageEncoderDecoderImpl();
        encoderDecoder.start();
        try {
            final Socket socket = new Socket();
            socket.connect(new InetSocketAddress("localhost", 25565));

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Message<?> message;
                    try {
                        if ((message = encoderDecoder.decode(new DataInputStream(socket.getInputStream()))) != null) {
                            if (message instanceof FileChatMessage) {
                                FileChatMessage fm = (FileChatMessage) message;
                                System.out.printf("<%s> [%s] {Sent file %s}%n",
                                        LocalDateTime.ofInstant(
                                                Instant.ofEpochMilli(fm.getTimestamp().getTimestamp()),
                                                ZoneId.systemDefault()
                                        ).toString(),
                                        fm.getSenderName().getString(),
                                        fm.getFileName().getString()
                                );
                                FileOutputStream fileOutputStream = new FileOutputStream(((FileChatMessage) message).getFileName().getString() + UUID.randomUUID().toString().substring(0, 10));
                                byte[] bytes = ((FileChatMessage) message).getBytes().getBytes();
                                fileOutputStream.write(bytes);
                                fileOutputStream.flush();
                                fileOutputStream.close();
                                System.out.println("Saved file");
                            } else if (message instanceof SimpleChatMessage){
                                SimpleChatMessage sm = (SimpleChatMessage) message;
                                System.out.printf("<%s> [%s]: %s%n",
                                        LocalDateTime.ofInstant(
                                                Instant.ofEpochMilli(sm.getTimestamp().getTimestamp()),
                                                ZoneId.systemDefault()
                                        ).toString(),
                                        sm.getSenderName().getString(),
                                        sm.getText().getString()
                                );
                            } else if (message instanceof PingRequest) {
                                encoderDecoder.encode(new PingResponse(), dos);
                            } else if (message instanceof SystemMessage) {
                                System.out.println(message);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String line = bufferedReader.readLine();
                try {
                    Message<?> message = decoder.decode(line).run(line);
                    encoderDecoder.encode(message, dos);
                    if (message instanceof ConnectionResetMessage) {
                        socket.close();
                        System.exit(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
