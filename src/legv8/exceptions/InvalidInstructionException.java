/*
 * @author: TrDoanh
 */
package legv8.exceptions;

public class InvalidInstructionException extends RuntimeException {
    public InvalidInstructionException(String message) {
        super(message);
    }

    public InvalidInstructionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidInstructionException(Throwable cause) {
        super(cause);
    }
}
