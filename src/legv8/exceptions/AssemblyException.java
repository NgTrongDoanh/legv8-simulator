/*
 * @author TrDoanh
 */
package legv8.exceptions;

public class AssemblyException extends RuntimeException {

    public AssemblyException(String message) {
        super(message);
    }

    public AssemblyException(String message, Throwable cause) {
        super(message, cause);
    }

    public AssemblyException(Throwable cause) {
        super(cause);
    }
}