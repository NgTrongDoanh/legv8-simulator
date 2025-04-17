package simulator.exceptions;

/**
 * Exception thrown when there is an error during assembly.
 * This could be due to syntax errors, semantic errors, or other issues.
 */
public class AssemblyException extends RuntimeException {
    public AssemblyException(String message) { super(message); }
    public AssemblyException(String message, Throwable cause) { super(message, cause); }
}