/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.exceptions;

/*
 * Custom exception class for handling invalid instruction errors in the LEGv8 CPU simulator.
 * This exception is thrown when an invalid instruction is encountered during
 * execution or assembly.
 */
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
