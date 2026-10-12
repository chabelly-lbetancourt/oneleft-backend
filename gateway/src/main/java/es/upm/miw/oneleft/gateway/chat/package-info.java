/**
 * Relay of the chat WebSocket (HU-008). The WebMVC gateway proxies HTTP but not WebSockets, so it accepts the
 * connection of the app on {@code /ws/chat}, checks its token like any request, and opens a second WebSocket to the
 * chat service with that token, passing the messages from one to the other.
 */
package es.upm.miw.oneleft.gateway.chat;
