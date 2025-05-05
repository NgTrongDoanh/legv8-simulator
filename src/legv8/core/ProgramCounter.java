/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.core;

import legv8.exceptions.InvalidPCException;
import legv8.util.ColoredLog;

/**
 * Simulates the Program Counter (PC) of the LEGv8 CPU.
 * The PC keeps track of the current instruction
 * address being executed.
 * It can be set to a new address, reset to the base address,
 * and provides the current address.
 * The PC is initialized to a base address of 0x400000.
 */
public class ProgramCounter {
   
    /** The base address for the program counter. */
    public static final long BASE_ADDRESS = 0x400000;

    /** The current address of the program counter. */
    private long currentAddress;


    // --- Constructor ---

    /**
     * Constructs a new ProgramCounter instance.
     * Initializes the current address to the base address.
     */
    public ProgramCounter() {
        this.currentAddress = BASE_ADDRESS;
        System.out.println(ColoredLog.SUCCESS + "Program Counter initialized. PC = 0x" + Long.toHexString(this.currentAddress));
    }
    

    // --- Public API ---

    /**
     * Returns the current address of the program counter.
     * @return The current address of the program counter.
     */
    public long getCurrentAddress() {
        return currentAddress;
    }
    
    /**
     * Sets the program counter to a new address.
     * Validates the address to ensure it is non-negative and word-aligned.
     * @param newAddress The new address to set the program counter to.
     * @throws InvalidPCException if the new address is negative or not word-aligned.
     */
    public void setAddress(long newAddress) {
        if (newAddress < 0) {
            throw new InvalidPCException("Attempt to set PC to negative address: " + newAddress, newAddress);
        }

        
        if (newAddress % 4 != 0) {
            System.err.printf(ColoredLog.WARNING + "Warning: Setting PC to non-word-aligned address 0x%X. Behavior might be undefined during fetch.\n", newAddress);
            throw new InvalidPCException("Attempt to set PC to non-word-aligned address: " + newAddress, newAddress);
        }

        this.currentAddress = newAddress;
        
        
        System.out.printf("%s(ProgramCounter) Set: 0x%X -> 0x%X\n", ColoredLog.INFO, this.currentAddress, newAddress);
    }
    
    /*
     * Resets the program counter to the base address.
     * This is typically used to restart the program execution.
     */
    public void reset() {
        setAddress(BASE_ADDRESS);
    }
    
    /*
     * Returns a string representation of the program counter.
     * This includes the current address in hexadecimal format.
     */
    @Override
    public String toString() {
        return String.format("PC=0x%08X", currentAddress);
    }
}