package simulator.storage;

import simulator.exceptions.MemoryAccessException; // Dùng exception chung
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class MemoryStorage {
    private final Map<Long, Long> memory; // Key: Byte Address, Value: 64-bit data

    public MemoryStorage() {
        this.memory = new HashMap<>();
        System.out.println("Data Memory Storage initialized.");
    }

    /** Gets value. Returns 0 for uninitialized addresses. */
    public long getValue(long address) {
        // Alignment check should be done by the Controller
        if (address < 0) {
            // This basic check can remain here or be solely in the controller
             throw new MemoryAccessException("Attempt to read negative memory address: 0x" + Long.toHexString(address));
        }
        return memory.getOrDefault(address, 0L);
    }

    /** Sets value directly. Bypasses control signals. Useful for init/testing. */
    public void setValue(long address, long value) {
        // Alignment check should be done by the Controller
        if (address < 0) {
             throw new MemoryAccessException("Attempt to write negative memory address: 0x" + Long.toHexString(address));
        }
        memory.put(address, value);
    }

    public void clear() {
        memory.clear();
    }

    /** Returns a map of the current memory contents. */
    public Map<Long, Long> getMemoryContents() {
        // Return a copy to prevent external modification? Maybe not necessary here.
        return memory;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Data Memory (Non-Zero 64-bit words):\n");
        Map<Long, Long> sortedMemory = new TreeMap<>(this.memory);
        if (sortedMemory.isEmpty()) {
            sb.append("  (Empty)\n");
        } else {
            for (Map.Entry<Long, Long> entry : sortedMemory.entrySet()) {
                 // Chỉ hiện các địa chỉ word-aligned (chia hết cho 8)
                 if (entry.getKey() % 8 == 0) {
                     // if (entry.getValue() != 0L) { // Bỏ comment nếu chỉ muốn xem giá trị khác 0
                         sb.append(String.format("  0x%08X : 0x%016X (%d)\n",
                                                  entry.getKey(), entry.getValue(), entry.getValue()));
                     // }
                 }
            }
        }
        return sb.toString();
    }
}