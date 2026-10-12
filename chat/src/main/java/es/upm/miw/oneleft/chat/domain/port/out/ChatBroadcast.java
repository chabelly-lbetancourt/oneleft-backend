package es.upm.miw.oneleft.chat.domain.port.out;

import es.upm.miw.oneleft.chat.domain.model.ChatMessage;

/**
 * Output port that takes a new message to everyone connected to its chat, whatever replica of the service they are
 * connected to.
 */
public interface ChatBroadcast {

    void send(ChatMessage message);
}
