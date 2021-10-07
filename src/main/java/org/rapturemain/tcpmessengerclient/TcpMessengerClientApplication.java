package org.rapturemain.tcpmessengerclient;


import lombok.SneakyThrows;
import org.rapturemain.tcpmessengermessageframework.message.MessageEncoderDecoderImpl;
import org.rapturemain.tcpmessengermessageframework.message.messages.*;
import org.rapturemain.tcpmessengermessageframework.message.messages.system.ConnectionResetMessage;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

public class TcpMessengerClientApplication {

    private volatile boolean terminating = false;

    private final MessageEncoderDecoderImpl encoderDecoder = new MessageEncoderDecoderImpl();
    private final UserInputDecoder decoder = new UserInputDecoder();

    public static void main(String[] args) {
        new TcpMessengerClientApplication().start();
    }

    @SneakyThrows
    private void start() {
        encoderDecoder.start();

        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));

        Socket socket = connect(br, bw);

        DataInputStream dis = null;
        DataOutputStream dos = null;
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            writeLine(bw, "Cannot create data streams of socket. Terminating");
            System.exit(1);
        }

        writeLine(bw, "Connected to the server");

        InputHandler inputHandler = startInputHandler(dis, dos, bw);

        boolean running = true;
        while (running && !terminating) {
            try {
                running = handleUserInput(dos, br, bw);
            } catch (IllegalArgumentException e) {
                writeLine(bw, "<Client message> " + e.getMessage());
            } catch (IOException e) {
                writeLine(bw, "Writing/reading error. Terminating");
                running = false;
            }
        }

        inputHandler.disable();
        socket.close();
    }

    private boolean handleUserInput(DataOutputStream dos, BufferedReader br, BufferedWriter bw) throws IOException {
        String line = br.readLine();

        if (line.matches("/raw( [0-9A-Fa-f]{2})+")) {
            writeRawBytes(line, dos);
            return true;
        }

        Message<?> message = decoder.decode(line).run(line);
        encoderDecoder.encode(message, dos);

        if (message instanceof ConnectionResetMessage) {
            writeLine(bw, "Bye");
            return false;
        }

        return true;
    }

    private InputHandler startInputHandler(DataInputStream dis, DataOutputStream dos, BufferedWriter bw) {
        InputHandler inputHandler = new InputHandler(encoderDecoder, dis, dos, bw);
        Consumer<Exception> onException = (e) -> {
            try {
                writeLine(bw, "Connection reset by server. Terminating");
                writeLine(bw, e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            terminating = true;
        };
        inputHandler.setOnEOF(onException);
        inputHandler.setOnIOE(onException);

        inputHandler.start();

        return inputHandler;
    }

    private void writeRawBytes(String text, DataOutputStream dataOutputStream) throws IOException {
        String[] parts = text.split(" ");
        byte[] bytes = new byte[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            String b = parts[i];
            byte res = 0;
            for (int j = 0; j < 2; j++) {
                res <<= 4;

                char c = b.charAt(j);
                if (c >= '0' && c <= '9') {
                    res |= c - '0';
                }
                if (c >= 'A' && c <= 'F') {
                    res |= 10 + c - 'A';
                }
                if (c >= 'a' && c <= 'f') {
                    res |= 10 + c - 'a';
                }
            }
            bytes[i - 1] = res;
        }
        dataOutputStream.write(bytes);
    }

    @SneakyThrows
    private Socket connect(BufferedReader r, BufferedWriter w) {
        while (true) {
            String line = r.readLine();

            if (line.matches("/exit")) {
                writeLine(w, "Bye");
                System.exit(0);
            }

            if (!line.matches("^/connect \\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3} \\d{1,5}$") &&
                    !line.matches("^/connect localhost \\d{1,5}$")) {
                writeLine(w, "<Client Message> You are not connected. Connect first using '/connect {IP Address} {Port}' or exit using '/exit'");
                continue;
            }

            String[] parts = line.split(" ");

            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(parts[1], Integer.parseInt(parts[2])));

                socket.setSoTimeout(30000);

                return socket;
            } catch (IOException e) {
                writeLine(w, String.format("Could not connect to %s:%s", parts[1], parts[2]));
            }
        }
    }

    private void writeLine(BufferedWriter bw, String text) throws IOException {
        bw.write(text);
        bw.newLine();
        bw.flush();
    }
}
