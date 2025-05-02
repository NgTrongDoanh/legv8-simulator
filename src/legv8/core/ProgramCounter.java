package legv8.core;

import legv8.exceptions.InvalidPCException;
import legv8.util.ColoredLog;

public class ProgramCounter {
    
    public static final long BASE_ADDRESS = 0x400000;
    private long currentAddress;

    public ProgramCounter() {
        this.currentAddress = BASE_ADDRESS;
        System.out.println(ColoredLog.SUCCESS + "Program Counter initialized. PC = 0x" + Long.toHexString(this.currentAddress));
    }
    
    public long getCurrentAddress() {
        return currentAddress;
    }
    
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
    
    
    public void reset() {
        setAddress(BASE_ADDRESS);
    }
    

    @Override
    public String toString() {
        return String.format("PC=0x%08X", currentAddress);
    }
}