package simulator.core;

import simulator.exceptions.MemoryAccessException;
import simulator.storage.MemoryStorage;

/**
 * Controls Data Memory access based on signals.
 * Enforces alignment.
 */
public class DataMemoryController {
    private final MemoryStorage storage;
    // Optional: Track last operation details

    public DataMemoryController(MemoryStorage storage) {
        if (storage == null) throw new IllegalArgumentException("MemoryStorage cannot be null.");
        this.storage = storage;
        System.out.println("Data Memory Controller initialized.");
    }

    /**
     * Accesses memory for read or write based on signals.
     * @param address Byte address (must be 64-bit aligned - multiple of 8).
     * @param writeData Data to write if memWrite is true.
     * @param memWrite Control signal.
     * @param memRead Control signal.
     * @return Data read from memory if memRead is true, otherwise 0.
     * @throws MemoryAccessException if address is negative, misaligned, or other access error.
     */
    public long accessMemory(long address, long writeData, boolean memWrite, boolean memRead) {
        // --- Validation ---
        if (address < 0) {
            throw new MemoryAccessException("Negative memory address accessed: 0x" + Long.toHexString(address));
        }
        // Enforce 64-bit (8-byte) alignment for LDUR/STUR
        if (address % 8 != 0) {
            throw new MemoryAccessException("Misaligned memory access (not multiple of 8): 0x" + Long.toHexString(address));
        }
        // Optional: Check for conflicting signals (MemWrite and MemRead both true?)

        // --- Action ---
        long readValue = 0L;
        if (memWrite) {
            // TODO: Add logging/state tracking for visualization
            // System.out.printf("DMC Write: [0x%X] <= 0x%X\n", address, writeData);
            try {
                storage.setValue(address, writeData);
            } catch (Exception e) { // Catch potential storage errors
                 throw new MemoryAccessException("Error during memory write at 0x" + Long.toHexString(address), e);
            }
        } else if (memRead) {
            // TODO: Add logging/state tracking for visualization
            try {
                readValue = storage.getValue(address);
                 // System.out.printf("DMC Read: [0x%X] => 0x%X\n", address, readValue);
            } catch (Exception e) {
                 throw new MemoryAccessException("Error during memory read at 0x" + Long.toHexString(address), e);
            }
        } else {
            // No memory operation requested
            // System.out.printf("DMC Inactive: Addr=0x%X\n", address);
        }
        return readValue;
    }

    /** Provides access to the underlying storage for display purposes. */
    public MemoryStorage getStorage() {
        return storage;
    }

     public void clearStorage() {
        storage.clear();
    }
}