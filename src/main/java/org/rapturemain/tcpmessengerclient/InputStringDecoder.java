package org.rapturemain.tcpmessengerclient;

import org.rapturemain.tcpmessengermessageframework.message.base.RawBytesEntry;
import org.rapturemain.tcpmessengermessageframework.message.base.StringEntry;
import org.rapturemain.tcpmessengermessageframework.message.messages.ConnectionResetMessage;
import org.rapturemain.tcpmessengermessageframework.message.messages.FileChatMessage;
import org.rapturemain.tcpmessengermessageframework.message.messages.SimpleChatMessage;
import org.rapturemain.tcpmessengermessageframework.message.messages.request.RegistrationRequestMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class InputStringDecoder {

    public Action decode(String text) {
        if (text.startsWith("/register ")) {
            return getRegisterAction();
        }
        if (text.startsWith("/attach ")) {
            return getAttachmentAction();
        }
        if (text.startsWith("/exit")) {
            return getResetConnectionAction();
        }
        return getSimpleMessageAction();
    }

    private Action getRegisterAction() {
        return text -> RegistrationRequestMessage.builder()
                .name(new StringEntry(text.split(" ")[1]))
                .build();
    }

    private Action getAttachmentAction() {
        return text -> {
            String path = text.split("'")[1];
            String[] fileNameParts = path.split("[\\\\/]");
            String fileName = fileNameParts[fileNameParts.length - 1];
            try {
                FileInputStream fileInputStream = new FileInputStream(new File(path));
                byte[] bytes = fileInputStream.readAllBytes();
                return FileChatMessage.builder()
                        .fileName(new StringEntry(fileName))
                        .bytes(new RawBytesEntry(bytes))
                        .build();
            } catch (IOException e) {
                throw new IllegalArgumentException();
            }
        };
    }

    private Action getSimpleMessageAction() {
        return text -> SimpleChatMessage.builder()
                .text(new StringEntry(text))
                .build();
    }

    private Action getResetConnectionAction() {
        return text -> new ConnectionResetMessage();
    }

}
