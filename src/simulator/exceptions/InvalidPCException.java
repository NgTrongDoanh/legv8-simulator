package simulator.exceptions;

/**
 * Exception thrown when an invalid instruction is encountered.
 * This could be due to an invalid opcode or an invalid instruction format.
 */
public class InvalidPCException extends RuntimeException {
    private final long invalidAddress;
    public InvalidPCException(String message, long invalidAddress) {
        super(message);
        this.invalidAddress = invalidAddress;
    }
    public long getInvalidAddress() { return invalidAddress; }
}