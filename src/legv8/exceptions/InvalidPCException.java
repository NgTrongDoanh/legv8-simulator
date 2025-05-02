
package legv8.exceptions;

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
