package legv8.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import legv8.exceptions.MemoryAccessException;
import legv8.util.ColoredLog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MemoryStorage {

    
    private final Map<Long, Byte> memory;
    public static final long MIN_ADDRESS = 0x500000; 
    public static final long VALUE_MASK = 0xFFFFFFFFFFFFFFFFL; // Sửa thành 64-bit    
    
    private static final ByteOrder ENDIANNESS = ByteOrder.LITTLE_ENDIAN;

    public MemoryStorage() {
        
        
        this.memory = new HashMap<>();
        
                        
    }

    public MemoryStorage(MemoryStorage initialMemory) {
        
        this.memory = new HashMap<>(initialMemory.memory);
        
    }

    private void checkAddress(long address) {
        if (address < MIN_ADDRESS) {
            throw new MemoryAccessException("Attempt to access memory below minimum allowed address (0x"
                                            + Long.toHexString(MIN_ADDRESS) + ")", address);
        }
    }

    public byte[] readBytes(long startAddress, int numBytes) {
        checkAddress(startAddress); 
        checkAddress(startAddress + numBytes - 1); 

        byte[] data = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            data[i] = this.memory.getOrDefault(startAddress + i, (byte) 0);
        }
        return data;
    }

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

    public long readLong(long address) {
        byte[] data = readBytes(address, 8); 
        return ByteBuffer.wrap(data).order(ENDIANNESS).getLong();
    }

    public void writeLong(long address, long value) {
        byte[] data = ByteBuffer.allocate(8).order(ENDIANNESS).putLong(value).array();
        writeBytes(address, data); 
    }

    public int readInt(long address) {
        byte[] data = readBytes(address, 4);
        return ByteBuffer.wrap(data).order(ENDIANNESS).getInt();
    }

    public void writeInt(long address, int value) {
        byte[] data = ByteBuffer.allocate(4).order(ENDIANNESS).putInt(value).array();
        writeBytes(address, data);
    }

    public short readShort(long address) {
        byte[] data = readBytes(address, 2);
        return ByteBuffer.wrap(data).order(ENDIANNESS).getShort();
    }

    public void writeShort(long address, short value) {
        byte[] data = ByteBuffer.allocate(2).order(ENDIANNESS).putShort(value).array();
        writeBytes(address, data);
    }

    public byte readByte(long address) {
        
        checkAddress(address); 
        return this.memory.getOrDefault(address, (byte) 0);
    }

    public void writeByte(long address, byte value) {
        checkAddress(address); 
        
        if (value == 0) {
            memory.remove(address);
        } else {
            memory.put(address, value);
        }
    }

    public void clear() {
        memory.clear();
        System.out.println(ColoredLog.SUCCESS + "Data Memory Storage cleared.");
    }

    public Map<Long, Byte> getMemoryContents() {
        
        return new HashMap<>(this.memory);
    }

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

        boolean contentFound = false;
        
        for (long address = startAddress; address <= endAddress; address += 8) {
                sb.append(String.format("  0x%08X : 0x%016X (%d)\n",
                                        address, readLong(address), readLong(address)));
                contentFound = true; 
            
        }

        System.out.println(ColoredLog.INFO + "Memory Storage Contents:\n");
        System.out.println(sb.toString());
    }

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