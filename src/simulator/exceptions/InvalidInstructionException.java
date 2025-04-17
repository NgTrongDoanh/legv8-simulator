package simulator.exceptions;

/**
 * Exception thrown when an invalid instruction is encountered.
 * This could be due to an invalid opcode or an invalid instruction format.
 */
public class InvalidInstructionException extends RuntimeException {
    public InvalidInstructionException(String message) { super(message); }
    public InvalidInstructionException(String message, Throwable cause) { super(message, cause); }
}