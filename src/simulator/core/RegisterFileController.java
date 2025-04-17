package simulator.core;

import simulator.storage.RegisterStorage;

/**
 * Controls Register Read/Write operations based on signals.
 * Enforces XZR rules.
 */
public class RegisterFileController {
    private final RegisterStorage storage;
    // Optional: Add fields to track last operation for GUI/debugging

    public RegisterFileController(RegisterStorage storage) {
        if (storage == null) throw new IllegalArgumentException("RegisterStorage cannot be null.");
        this.storage = storage;
        System.out.println("Register File Controller initialized.");
    }

    /** Reads two registers simultaneously. */
    public long[] readRegisters(int readReg1, int readReg2) {
        // Validation delegated to storage.getValue()
        long data1 = storage.getValue(readReg1);
        long data2 = storage.getValue(readReg2);
        // TODO: Add logging or state tracking if needed for visualization
        return new long[]{data1, data2};
    }

    /** Reads a single register. */
     public long readSingleRegister(int readReg) {
         return storage.getValue(readReg);
     }

    /** Writes to a register IF regWrite is true AND target is not XZR. */
    public void writeRegister(int writeReg, long writeData, boolean regWrite) {
        if (regWrite) {
            if (writeReg == RegisterStorage.ZERO_REGISTER_INDEX) {
                // Log attempt to write to XZR (ignored)
                // System.out.println("RFController Info: Ignored write to XZR.");
            } else {
                try {
                    storage.setValue(writeReg, writeData); // Perform the write
                    // Log successful write
                    // System.out.printf("RFController Write: X%d <= 0x%016X\n", writeReg, writeData);
                } catch (IllegalArgumentException e) {
                    // Handle potential error from storage (though unlikely if writeReg checked)
                    System.err.println("RFController Error writing: " + e.getMessage());
                    // Rethrow or handle as appropriate for simulation
                    throw e;
                }
            }
        }
        // Else: regWrite is false, do nothing.
    }

    /** Provides access to the underlying storage for display purposes. */
    public RegisterStorage getStorage() {
        return storage;
    }

    public void clearStorage() {
        storage.clear();
    }
}