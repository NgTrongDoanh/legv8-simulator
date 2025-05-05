/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.exceptions;

/**
 * Custom exception class for handling invalid program counter (PC) errors in the LEGv8 CPU simulator.
 * This exception is thrown when an attempt is made to set the PC to an invalid address,
 * such as a negative or non-word-aligned address.
 */
public class InvalidPCException extends RuntimeException {

    private final long invalidAddress;

    public InvalidPCException(String message, long invalidAddress) {
        super(message);
        this.invalidAddress = invalidAddress;
    }

    public InvalidPCException(String message, Throwable cause, long invalidAddress) {
        super(message, cause);
        this.invalidAddress = invalidAddress;
    }

    public InvalidPCException(Throwable cause, long invalidAddress) {
        super(cause);
        this.invalidAddress = invalidAddress;
    }

    public long getInvalidAddress() {
        return invalidAddress;
    }
}
