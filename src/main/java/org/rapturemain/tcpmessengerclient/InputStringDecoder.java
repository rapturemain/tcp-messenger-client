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
        if (text.matches("^/register [^\\s]*$")) {
            return getRegisterAction();
        }
        if (text.matches("^/attach '.*'$")) {
            return getAttachmentAction();
        }
        if (text.matches("/exit")) {
            return getResetConnectionAction();
        }
        if (text.startsWith("/")) {
            throw new IllegalArgumentException("Command is wrong");
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
            try {
                File file = new File(path);
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] bytes = fileInputStream.readAllBytes();
                return FileChatMessage.builder()
                        .fileName(new StringEntry(file.getName()))
                        .bytes(new RawBytesEntry(bytes))
                        .build();
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read file " + path);
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
