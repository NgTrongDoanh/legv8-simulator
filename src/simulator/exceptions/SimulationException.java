package simulator.exceptions;

/**
 * A general exception indicating an error occurred during simulation execution,
 * potentially wrapping more specific exceptions.
 */
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

    public long getErrorPC() {
        return errorPC;
    }

    public long getErrorCycle() {
        return errorCycle;
    }

    @Override
    public String getMessage() {
        return String.format("Simulation Error (Cycle: %d, PC: 0x%X): %s",
                             errorCycle, errorPC, super.getMessage());
    }
}