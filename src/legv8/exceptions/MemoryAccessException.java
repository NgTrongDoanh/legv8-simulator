/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.exceptions;


/**
 * Custom exception class for handling memory access errors in the LEGv8 CPU simulator.
 * This exception is thrown when there are issues accessing memory, such as invalid addresses
 * or memory protection violations.
 */
public class MemoryAccessException extends RuntimeException {

    private final long address;

    public MemoryAccessException(String message, long address) {
        super(message + ": 0x" + Long.toHexString(address));
        this.address = address;
    }

    public MemoryAccessException(String message, Throwable cause, long address) {
        super(message + ": 0x" + Long.toHexString(address), cause);
        this.address = address;
    }

    public MemoryAccessException(Throwable cause, long address) {
        super(cause);
        this.address = address;
    }

    public long getAddress() {
        return address;
    }
    
}
