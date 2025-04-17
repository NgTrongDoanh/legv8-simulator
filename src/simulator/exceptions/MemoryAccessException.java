package simulator.exceptions;

/**
 * Exception thrown when there is an error accessing memory.
 * This could be due to an invalid address or a memory access violation.
 */
public class MemoryAccessException extends RuntimeException {
    public MemoryAccessException(String message) { super(message); }
    public MemoryAccessException(String message, Throwable cause) { super(message, cause); }
}