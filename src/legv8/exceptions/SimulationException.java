/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.exceptions;

/**
 * Custom exception class for handling simulation-related errors in the LEGv8 CPU simulator.
 * This exception is thrown when there are issues during the simulation process,
 * such as invalid program counter (PC) values or other simulation errors.
 */
public class SimulationException extends Exception {
    
    // The program counter (PC) at which the error occurred
    private final long errorPC;

    // The cycle number at which the error occurred
    private final long errorCycle;

    public SimulationException(String message, long errorPC, long errorCycle) {
        super(message);
        this.errorPC = errorPC;
        this.errorCycle = errorCycle;
    }

    public SimulationException(String message, Throwable cause, long errorPC, long errorCycle) {
        super(message, cause);
        this.errorPC = errorPC;
        this.errorCycle = errorCycle;
    }

    public SimulationException(Throwable cause, long errorPC, long errorCycle) {
        super(cause);
        this.errorPC = errorPC;
        this.errorCycle = errorCycle;
    }

    public long getErrorPC() {
        return errorPC;
    }
    
    public long getErrorCycle() {
        return errorCycle;
    }

    @Override
    public String toString() {
        return String.format("SimulationException (PC: 0x%X, Cycle: %d): %s", errorPC, errorCycle, super.getMessage());
    }
    
}
