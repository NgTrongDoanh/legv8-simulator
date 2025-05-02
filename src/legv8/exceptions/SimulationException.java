package legv8.exceptions;

public class SimulationException extends Exception {
    
    private final long errorPC;
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
