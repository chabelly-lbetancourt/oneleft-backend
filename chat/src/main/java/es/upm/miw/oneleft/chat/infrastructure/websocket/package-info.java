/**
 * WebSocket adapter of the chat (HU-008): {@code /ws/chat?planId=}, opened by the gateway with the token of whoever
 * connects. Messages are JSON: the client sends {@code {"text": "..."}} and receives messages, errors and the state of
 * the chat.
 */
package es.upm.miw.oneleft.chat.infrastructure.websocket;
