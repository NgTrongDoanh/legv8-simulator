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
     * Reads or writes a byte (8 bits).
     * @param address The 64-bit byte address for the memory access (typically from ALU result).
     * @param writeData The 64-bit data to write to memory if MemWrite is asserted (typically from register file).
     * @param memWrite Control signal: If true, performs a memory write.
     * @param memRead Control signal: If true, performs a memory read.
     * @return The byte (8-bit) data read from memory if MemRead is true, otherwise 0.
     * @throws MemoryAccessException if the address is invalid (e.g., negative), misaligned,
     *                                 or if both MemRead and MemWrite are asserted simultaneously.
     */
    public byte accessMemory_byte(long address, long writeData, boolean memWrite, boolean memRead) {
        if (address < MemoryStorage.MIN_ADDRESS) {
            throw new MemoryAccessException("Negative memory address accessed", address);
        }
        
        if (memWrite && memRead) {
            throw new MemoryAccessException("Conflicting memory access signals (both MemWrite and MemRead are true)", address);
        }
        
        byte readValue = 0;
        byte byteWriteData = (byte) (writeData & 0xFF); 
        if (memWrite) {
            System.out.printf("%s(DataMemoryControl) Write: [0x%X] <= 0x%X\n", ColoredLog.INFO, address, writeData);
            try {
                storage.writeByte(address, byteWriteData);
            } catch (Exception e) {
                throw new MemoryAccessException("Error during memory write at 0x", e, address);
            }
        } else if (memRead) {
            try {
                readValue = storage.readByte(address);
                System.out.printf("%s(DataMemoryControl) Read: [0x%X] => 0x%X\n", ColoredLog.INFO, address, readValue);
            } catch (Exception e) {
                throw new MemoryAccessException("Error during memory read at 0x", e, address);
            }
        } else {
            System.out.printf("%s(DataMemoryControl) Inactive: Addr=0x%X\n",ColoredLog.INFO, address);
        }
        return readValue;
    }

    /**
     * Performs a memory access operation (read or write or neither) based on control signals.
     * Reads or writes a halfword (16 bits).
     * @param address The 64-bit byte address for the memory access (typically from ALU result).
     * @param writeData The 64-bit data to write to memory if MemWrite is asserted (typically from register file).
     * @param memWrite Control signal: If true, performs a memory write.
     * @param memRead Control signal: If true, performs a memory read.
     * @return The 16-bit data read from memory if MemRead is true, otherwise 0.
     * @throws MemoryAccessException if the address is invalid (e.g., negative), misaligned,
     *                                 or if both MemRead and MemWrite are asserted simultaneously.
     */
    public short accessMemory_halfword(long address, long writeData, boolean memWrite, boolean memRead) {
        if (address < MemoryStorage.MIN_ADDRESS) {
            throw new MemoryAccessException("Negative memory address accessed", address);
        }
        
        if (memWrite && memRead) {
            throw new MemoryAccessException("Conflicting memory access signals (both MemWrite and MemRead are true)", address);
        }
        
        if (address % 2 != 0) {
            throw new MemoryAccessException("Unaligned halfword access", address);
        }

        short readValue = 0;
        short halfwordWriteData = (short) (writeData & 0xFFFF); 
        if (memWrite) {
            System.out.printf("%s(DataMemoryControl) Write: [0x%X] <= 0x%X\n", ColoredLog.INFO, address, writeData);
            try {
                storage.writeHalfWord(address, halfwordWriteData);
            } catch (Exception e) {
                throw new MemoryAccessException("Error during memory write at 0x", e, address);
            }
        } else if (memRead) {
            try {
                readValue = storage.readHalfWord(address);
                System.out.printf("%s(DataMemoryControl) Read: [0x%X] => 0x%X\n", ColoredLog.INFO, address, readValue);
            } catch (Exception e) {
                throw new MemoryAccessException("Error during memory read at 0x", e, address);
            }
        } else {
            System.out.printf("%s(DataMemoryControl) Inactive: Addr=0x%X\n",ColoredLog.INFO, address);
        }
        return readValue;
    }

    /**
     * Performs a memory access operation (read or write or neither) based on control signals.
     * Reads or writes a word (32 bits).
     * @param address The 64-bit byte address for the memory access (typically from ALU result).
     * @param writeData The 64-bit data to write to memory if MemWrite is asserted (typically from register file).
     * @param memWrite Control signal: If true, performs a memory write.
     * @param memRead Control signal: If true, performs a memory read.
     * @return The 32-bit data read from memory if MemRead is true, otherwise 0.
     * @throws MemoryAccessException if the address is invalid (e.g., negative), misaligned,
     *                                 or if both MemRead and MemWrite are asserted simultaneously.
     */
    public int accessMemory_word(long address, long writeData, boolean memWrite, boolean memRead) {
        if (address < MemoryStorage.MIN_ADDRESS) {
            throw new MemoryAccessException("Negative memory address accessed", address);
        }
        
        if (memWrite && memRead) {
            throw new MemoryAccessException("Conflicting memory access signals (both MemWrite and MemRead are true)", address);
        }
        
        if (address % 4 != 0) {
            throw new MemoryAccessException("Unaligned word access", address);
        }

        int readValue = 0;
        int wordWriteData = (int) (writeData & 0xFFFFFFFF); 
        if (memWrite) {
            System.out.printf("%s(DataMemoryControl) Write: [0x%X] <= 0x%X\n", ColoredLog.INFO, address, writeData);
            try {
                storage.writeWord(address, wordWriteData);
            } catch (Exception e) {
                throw new MemoryAccessException("Error during memory write at 0x", e, address);
            }
        } else if (memRead) {
            try {
                readValue = storage.readWord(address);
                System.out.printf("%s(DataMemoryControl) Read: [0x%X] => 0x%X\n", ColoredLog.INFO, address, readValue);
            } catch (Exception e) {
                throw new MemoryAccessException("Error during memory read at 0x", e, address);
            }
        } else {
            System.out.printf("%s(DataMemoryControl) Inactive: Addr=0x%X\n",ColoredLog.INFO, address);
        }
        return readValue;
    }

    /**
     * Performs a memory access operation (read or write or neither) based on control signals.
     * Reads or writes a double word (64 bits).
     * @param address The 64-bit byte address for the memory access (typically from ALU result).
     * @param writeData The 64-bit data to write to memory if MemWrite is asserted (typically from register file).
     * @param memWrite Control signal: If true, performs a memory write.
     * @param memRead Control signal: If true, performs a memory read.
     * @return The 64-bit data read from memory if MemRead is true, otherwise 0.
     * @throws MemoryAccessException if the address is invalid (e.g., negative), misaligned,
     *                                 or if both MemRead and MemWrite are asserted simultaneously.
     */
    public long accessMemory_doubleWord(long address, long writeData, boolean memWrite, boolean memRead) {
        if (address < MemoryStorage.MIN_ADDRESS) {
            throw new MemoryAccessException("Negative memory address accessed", address);
        }
        
        if (memWrite && memRead) {
            throw new MemoryAccessException("Conflicting memory access signals (both MemWrite and MemRead are true)", address);
        }

        if (address % 8 != 0) {
            throw new MemoryAccessException("Unaligned double word access", address);
        }
        
        long readValue = 0;
        if (memWrite) {
            System.out.printf("%s(DataMemoryControl) Write: [0x%X] <= 0x%X\n", ColoredLog.INFO, address, writeData);
            try {
                storage.writeDoubleWord(address, writeData);
            } catch (Exception e) {
                throw new MemoryAccessException("Error during memory write at 0x", e, address);
            }
        } else if (memRead) {
            try {
                readValue = storage.readDoubleWord(address);
                System.out.printf("%s(DataMemoryControl) Read: [0x%X] => 0x%X\n", ColoredLog.INFO, address, readValue);
            } catch (Exception e) {
                throw new MemoryAccessException("Error during memory read at 0x", e, address);
            }
        } else {
            System.out.printf("%s(DataMemoryControl) Inactive: Addr=0x%X\n",ColoredLog.INFO, address);
        }
        return readValue;
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