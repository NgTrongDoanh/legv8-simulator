/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.core;

import legv8.exceptions.MemoryAccessException;
import legv8.storage.MemoryStorage;
import legv8.util.ColoredLog;

import java.util.Objects;

/**
 * Controller for accessing the Data Memory storage unit.
 * Acts as an interface between the CPU datapath (ALU for address, Register File for data)
 * and the underlying {@link MemoryStorage}. It orchestrates read and write operations
 * based on control signals (MemRead, MemWrite) and handles address validation and alignment checks.
 */
public class DataMemoryController {

    /** The underlying storage mechanism for data memory. */
    private final MemoryStorage storage;


    // --- Constructor ---

    /**
     * Constructs a DataMemoryController associated with a given {@link MemoryStorage} instance.
     * @param storage The MemoryStorage instance to manage. Must not be null.
     * @throws NullPointerException if storage is null.
     */
    public DataMemoryController(MemoryStorage storage) {
        this.storage = Objects.requireNonNull(storage, "MemoryStorage cannot be null.");
        System.out.println(ColoredLog.SUCCESS + "Data Memory Controller initialized.");
    }


    // --- Public API ---

    /**
     * Performs a memory access operation (read or write or neither) based on control signals.
     * Reads or writes a 64-bit long value (doubleword).
     * Enforces 8-byte alignment for LDUR/STUR operations implicitly via the underlying storage checks.
     * Validates control signals to prevent simultaneous read and write.
     *
     * @param address The 64-bit byte address for the memory access (typically from ALU result).
     * @param writeData The 64-bit data to write to memory if MemWrite is asserted (typically from RF ReadData2).
     * @param memWrite Control signal: If true, performs a memory write.
     * @param memRead Control signal: If true, performs a memory read.
     * @return The 64-bit data read from memory if MemRead is true, otherwise returns 0L.
     * @throws MemoryAccessException if the address is invalid (e.g., negative), misaligned,
     *                              or if both MemRead and MemWrite are asserted simultaneously.
     */
    public long accessMemory(long address, long writeData, boolean memWrite, boolean memRead) {
        if (address < MemoryStorage.MIN_ADDRESS && (memRead || memWrite)) {
            throw new MemoryAccessException("Negative memory address accessed", address);
        }
        
        if (memWrite && memRead) {
            throw new MemoryAccessException("Conflicting memory access signals (both MemWrite and MemRead are true)", address);
        }
        
        long readValue = 0L;
        writeData = writeData & MemoryStorage.VALUE_MASK; 
        if (memWrite) {
            System.out.printf("%s(DataMemoryControl) Write: [0x%X] <= 0x%X\n", ColoredLog.INFO, address, writeData);
            try {
                storage.writeLong(address, writeData);
            } catch (Exception e) { 
                throw new MemoryAccessException("Error during memory write at 0x", e, address);
            }
        } else if (memRead) {
            try {
                readValue = storage.readLong(address);
                System.out.printf("%(DataMemoryControl) Read: [0x%X] => 0x%X\n", ColoredLog.INFO, address, readValue);
            } catch (Exception e) {
                throw new MemoryAccessException("Error during memory read at 0x", e, address);
            }
        } else {
            System.out.printf("%s(DataMemoryControl) Inactive: Addr=0x%X\n",ColoredLog.INFO, address);
        }
        return readValue & MemoryStorage.VALUE_MASK; 
    }

    /**
     * Retrieves the underlying MemoryStorage instance.
     * @return A new MemoryStorage instance representing the current state of the storage.
     */
    public MemoryStorage getStorage() {
        return new MemoryStorage(this.storage);
    }

    /**
     * Clears the entire memory storage.
     * This method is typically used for resetting or initializing the memory state.
     */
    public void clearStorage() {
        storage.clear();
    }
}