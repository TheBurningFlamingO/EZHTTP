package Data;

/**
 * The {@code ServerState} enum represents the various states that a server can be in.
 * It is used to track and manage the operational status of the server during its lifecycle.
 *
 * Enum Constants:
 * - {@code OPEN} represents a state where the server is actively running and accepting connections.
 * - {@code CLOSED} represents a state where the server is not operational or has been terminated.
 * - {@code ERROR} represents a state where the server has encountered an error and may not function properly.
 *
 * This enum is typically used in conjunction with the {@code EZHTTPServer} class to set
 * and retrieve the current state of the server, ensuring proper actions are taken based
 * on the server's status.
 */
public enum ServerState {
    OPEN,
    CLOSED,
    ERROR;

}
