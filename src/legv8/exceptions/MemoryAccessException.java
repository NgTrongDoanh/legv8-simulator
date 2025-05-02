package legv8.exceptions;

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
