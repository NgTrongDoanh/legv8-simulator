package simulator.core;

import simulator.exceptions.InvalidPCException;

public class ProgramCounter {
    // Nên đặt BASE_ADDRESS ở một nơi tập trung hơn, ví dụ Constants class
    public static final long BASE_ADDRESS = 0x400000;
    private long currentAddress;

    public ProgramCounter() {
        this.currentAddress = BASE_ADDRESS;
        System.out.println("Program Counter initialized. PC = 0x" + Long.toHexString(this.currentAddress));
    }

    public long getCurrentAddress() {
        return currentAddress;
    }

    /**
     * Sets the PC to a new address. Used for branches/jumps/reset.
     * Throws exception if address is negative or misaligned.
     */
    public void setAddress(long newAddress) {
        if (newAddress < 0) {
            throw new InvalidPCException("Attempt to set PC to negative address: " + newAddress, newAddress);
        }
        // Mặc dù fetch sẽ kiểm tra lại, việc kiểm tra ở đây giúp phát hiện lỗi sớm hơn
        if (newAddress % 4 != 0) {
             System.err.printf("Warning: Setting PC to non-word-aligned address 0x%X. Behavior might be undefined during fetch.\n", newAddress);
             // Có thể ném lỗi ở đây nếu muốn chặt chẽ hơn:
             // throw new InvalidPCException("Attempt to set PC to non-word-aligned address: " + newAddress, newAddress);
        }
        // System.out.printf("PC Set: 0x%X -> 0x%X\n", this.currentAddress, newAddress); // Log có thể chuyển vào Simulator
        this.currentAddress = newAddress;
    }

    /** Resets PC to the base address. */
    public void reset() {
        setAddress(BASE_ADDRESS);
    }

    @Override
    public String toString() {
        return String.format("PC=0x%08X", currentAddress);
    }
}