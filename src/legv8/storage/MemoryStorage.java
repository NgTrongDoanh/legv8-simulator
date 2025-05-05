/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.storage;

import legv8.exceptions.MemoryAccessException;
import legv8.util.ColoredLog;

 import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * MemoryStorage is a class that represents a memory storage system for the LEGv8 architecture.
 * It provides methods to read and write data to memory, including bytes, shorts, ints, and longs.
 * The memory is represented as a map with addresses as keys and byte values.
 */
public class MemoryStorage {
    // --- Fields ---
    // A map to store memory contents, where the key is the address and the value is the byte at that address   
    private final Map<Long, Byte> memory;
    // The minimum address for memory access
    public static final long MIN_ADDRESS = 0x500000; 
    // Mask for 64-bit values
    public static final long VALUE_MASK = 0xFFFFFFFFFFFFFFFFL; 
    
    // The endianness of the memory storage (little-endian)
    // This is the default for LEGv8 architecture
    private static final ByteOrder ENDIANNESS = ByteOrder.LITTLE_ENDIAN;

    // --- Constructor ---
    /**
     * Constructor for MemoryStorage.
     * Initializes an empty memory storage.
     */
    public MemoryStorage() {
        this.memory = new HashMap<>();
    }

    /**
     * Constructor for MemoryStorage.
     * Initializes memory storage with the contents of another MemoryStorage instance.
     * @param initialMemory The initial memory storage to copy from.
     */
    public MemoryStorage(MemoryStorage initialMemory) { 
        this.memory = new HashMap<>(initialMemory.memory);
    }


    // --- Memory Access Methods ---
    /**
     * Checks if the given address is within the allowed range.
     * @param address The address to check.
     * @throws MemoryAccessException if the address is below the minimum allowed address.
     */
    private void checkAddress(long address) {
        if (address < MIN_ADDRESS) {
            throw new MemoryAccessException("Attempt to access memory below minimum allowed address (0x"
                                            + Long.toHexString(MIN_ADDRESS) + ")", address);
        }
    }

    /**
     * Reads a specified number of bytes from memory starting at the given address.
     * @param startAddress The starting address to read from.
     * @param numBytes The number of bytes to read.
     * @return A byte array containing the read data.
     */
    public byte[] readBytes(long startAddress, int numBytes) {
        checkAddress(startAddress); 
        checkAddress(startAddress + numBytes - 1); 

        byte[] data = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            data[i] = this.memory.getOrDefault(startAddress + i, (byte) 0);
        }
       
        return data;
    }

    /**
     * Writes a byte array to memory starting at the given address.
     * @param startAddress The starting address to write to.
     * @param data The byte array to write.
     */
    public void writeBytes(long startAddress, byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data to write cannot be null or empty.");
        }
       
        int numBytes = data.length;
        checkAddress(startAddress); 
        checkAddress(startAddress + data.length - 1); 

        for (int i = 0; i < numBytes; i++) {
            if (data[i] == 0) {
                memory.remove(startAddress + i);
            } else {
                memory.put(startAddress + i, data[i]);
            }
        }
    }

    /**
     * Reads a long value (8 bytes) from memory starting at the given address.
     * @param address The address to read from.
     * @return The long value read from memory.
     */
    public long readLong(long address) {
        byte[] data = readBytes(address, 8); 
        return ByteBuffer.wrap(data).order(ENDIANNESS).getLong();
    }

    /**
     * Writes a long value (8 bytes) to memory starting at the given address.
     * @param address The address to write to.
     * @param value The long value to write.
     */
    public void writeLong(long address, long value) {
        byte[] data = ByteBuffer.allocate(8).order(ENDIANNESS).putLong(value).array();
        writeBytes(address, data); 
    }

    /**
     * Reads an integer value (4 bytes) from memory starting at the given address.
     * @param address The address to read from.
     * @return The integer value read from memory.
     */
    public int readInt(long address) {
        byte[] data = readBytes(address, 4);
        return ByteBuffer.wrap(data).order(ENDIANNESS).getInt();
    }

    /**
     * Writes an integer value (4 bytes) to memory starting at the given address.
     * @param address The address to write to.
     * @param value The integer value to write.
     */
    public void writeInt(long address, int value) {
        byte[] data = ByteBuffer.allocate(4).order(ENDIANNESS).putInt(value).array();
        writeBytes(address, data);
    }

    /**
     * Reads a short value (2 bytes) from memory starting at the given address.
     * @param address The address to read from.
     * @return The short value read from memory.
     */
    public short readShort(long address) {
        byte[] data = readBytes(address, 2);
        return ByteBuffer.wrap(data).order(ENDIANNESS).getShort();
    }

    /**
     * Writes a short value (2 bytes) to memory starting at the given address.
     * @param address The address to write to.
     * @param value The short value to write.
     */
    public void writeShort(long address, short value) {
        byte[] data = ByteBuffer.allocate(2).order(ENDIANNESS).putShort(value).array();
        writeBytes(address, data);
    }

    /**
     * Reads a byte value from memory at the given address.
     * @param address The address to read from.
     * @return The byte value read from memory.
     */
    public byte readByte(long address) { 
        checkAddress(address); 
        return this.memory.getOrDefault(address, (byte) 0);
    }

    /**
     * Writes a byte value to memory at the given address.
     * @param address The address to write to.
     * @param value The byte value to write.
     */
    public void writeByte(long address, byte value) {
        checkAddress(address); 

        if (value == 0) memory.remove(address);
        else memory.put(address, value);
    }

    /**
     * Returns the contents of the memory as a map.
     * @return A map containing the memory contents, where the key is the address and the value is the byte at that address.
     *         The map is a copy of the original memory map.
     */
    public Map<Long, Byte> getMemoryContents() {
        return new HashMap<>(this.memory);
    }

    /**
     * Returns the contents of the memory as a map of long values.
     * @param startAddress The starting address to read from.
     * @param endAddress The ending address to read to.
     * @return A map containing the memory contents, where the key is the address and the value is the long value at that address.
     */
    public Map<Long, Long> getMemoryContentsLong(long startAddress, long endAddress) {
        checkAddress(startAddress); 
        checkAddress(endAddress);
        
        if (startAddress > endAddress) {
            throw new MemoryAccessException("Memory range error: Start address (0x" + Long.toHexString(startAddress)
                                            + ") is greater than end address (0x" + Long.toHexString(endAddress) + ")", startAddress);
        }
        if (endAddress - startAddress > 0x1000) {
            throw new MemoryAccessException("Memory range error: Range too large (0x" + Long.toHexString(endAddress - startAddress)
                                            + " bytes).", startAddress);
        }
        
        Map<Long, Long> longMemory = new HashMap<>();
        for (long address = startAddress; address <= endAddress; address += 8) {
            longMemory.put(address, readLong(address));
        }

        return longMemory;
    }

    
    // --- Utility Methods ---

    /**
     * Clears the memory storage.
     */
    public void clear() {
        memory.clear();
        System.out.println(ColoredLog.SUCCESS + "Data Memory Storage cleared.");
    }

    /**
     * Displays the contents of the memory storage in a formatted manner.
     * @param startAddress The starting address to display.
     * @param endAddress The ending address to display.
     */
    public void displayMemoryContents(long startAddress, long endAddress) {
        checkAddress(startAddress); 
        checkAddress(endAddress);
        if (startAddress > endAddress) {
            throw new MemoryAccessException("Display range error: Start address (0x" + Long.toHexString(startAddress)
                                            + ") is greater than end address (0x" + Long.toHexString(endAddress) + ")", startAddress);
        }

        StringBuilder sb = new StringBuilder("Data Memory Contents (Bytes from 0x")
                .append(Long.toHexString(startAddress))
                .append(" to 0x")
                .append(Long.toHexString(endAddress))
                .append("):\n");
        
        for (long address = startAddress; address <= endAddress; address += 8) {
            sb.append(String.format("  0x%08X : 0x%016X (%d)\n",
                                    address, readLong(address), readLong(address)));
        }

        System.out.println(ColoredLog.INFO + "Memory Storage Contents:\n");
        System.out.println(sb.toString());
    }

    /**
     * Returns a string representation of the memory storage.
     * @return A string representation of the memory storage.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Data Memory (Initialized Bytes, Sorted):\n"); 
        Map<Long, Byte> sortedMemory = new TreeMap<>(this.memory);

        if (sortedMemory.isEmpty()) {
            sb.append("  (Empty)\n");
        } else {
            for (Map.Entry<Long, Byte> entry : sortedMemory.entrySet()) {
                sb.append(String.format("  0x%08X : 0x%02X (%d)\n",
                                        entry.getKey(), entry.getValue(), entry.getValue()));        
            }
        }
        
        return sb.toString();
    }
}