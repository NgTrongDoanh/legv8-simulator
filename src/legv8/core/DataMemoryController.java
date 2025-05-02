package legv8.core;

import java.util.Objects;

import legv8.exceptions.MemoryAccessException;
import legv8.storage.MemoryStorage;
import legv8.util.ColoredLog;

public class DataMemoryController {
    private final MemoryStorage storage;

    public DataMemoryController(MemoryStorage storage) {
        this.storage = Objects.requireNonNull(storage, "MemoryStorage cannot be null.");
        System.out.println(ColoredLog.SUCCESS + "Data Memory Controller initialized.");
    }

    public long accessMemory(long address, long writeData, boolean memWrite, boolean memRead) {
        
        if (address < MemoryStorage.MIN_ADDRESS && (memRead || memWrite)) {
            throw new MemoryAccessException("Negative memory address accessed", address);
        }
        // Enforce 64-bit (8-byte) alignment for LDUR/STUR
        
        
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
  
    public MemoryStorage getStorage() {
        return new MemoryStorage(this.storage);
    }

    public void clearStorage() {
        storage.clear();
    }
}