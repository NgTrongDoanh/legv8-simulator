/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.exceptions;

/**
 * Custom exception class for handling assembly-related errors in the LEGv8 CPU simulator.
 * This exception is thrown when there are issues during the assembly process,
 * such as invalid instructions or assembly failures.
 */
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