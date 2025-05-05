package legv8.core;

import legv8.storage.RegisterStorage;
import legv8.util.ColoredLog;

import java.util.Objects;

/**
 * Controller for accessing the Register File storage unit.
 * Acts as an interface between the CPU datapath (ALU for address, Register File for data)
 * and the underlying {@link RegisterStorage}. It orchestrates read and write operations
 * based on control signals (RegWrite) and handles register validation.
 */
public class RegisterFileController {

    /** The underlying storage mechanism for register file. */
    private final RegisterStorage storage;


    // --- Constructor ---

    /**
     * Constructs a RegisterFileController associated with a given {@link RegisterStorage} instance.
     * @param storage The RegisterStorage instance to manage. Must not be null.
     * @throws NullPointerException if storage is null.
     */
    public RegisterFileController(RegisterStorage storage) {
        this.storage = Objects.requireNonNull(storage, ColoredLog.WARNING + "RegisterStorage cannot be null.");
        System.out.println(ColoredLog.SUCCESS + "Register File Controller initialized.");
    }


    // --- Public API ---
    
    /**
     * Reads a register value from the register file.
     * @param readReg The index of the register to read (0-31).
     * @return The value of the specified register.
     */
    public long readRegister(int readReg) {
        return storage.getValue(readReg);
    }

    /**
     * Writes a value to a register in the register file.
     * @param writeReg The index of the register to write (0-31).
     * @param writeData The data to write to the register.
     * @param regWrite Control signal: If true, performs a register write.
     */
    // Note: writeData is masked to ensure it fits within the register size.
    public void writeRegister(int writeReg, long writeData, boolean regWrite) {
        writeData = writeData & RegisterStorage.VALUE_MASK; 
        if (regWrite) {
            if (writeReg == RegisterStorage.ZERO_REGISTER_INDEX) {
                
                System.out.println(ColoredLog.INFO + "(RegisterFileController) Info: Ignored write to XZR.");
            } else {
                try {
                    storage.setValue(writeReg, writeData); 
                    
                    System.out.printf("%s(RegisterFileController) Write: X%d <= 0x%016X\n", ColoredLog.INFO, writeReg, writeData);
                } catch (IllegalArgumentException e) {
                    
                    System.err.println(ColoredLog.ERROR + "(RegisterFileController) Error writing: " + e.getMessage());
                    
                    throw e;
                }
            }
        }
        
    }

    /**
     * Returns the underlying RegisterStorage instance.
     * @return The RegisterStorage instance.
     */
    // Note: this method returns a new instance of RegisterStorage to prevent external modifications.
    public RegisterStorage getStorage() {
        return new RegisterStorage(storage);
    }

    /**
     * Clears the register storage.
     * This method is typically used for resetting the register file state.
     */
    public void clearStorage() {
        storage.clear();
    }

    /**
     * Displays the current state of the register file.
     * This method is typically used for debugging purposes.
     */
    public void displayStorage() {
        System.out.println(ColoredLog.INFO + "Register File State:");
        System.out.println("--- Registers Content ---");
        System.out.println(storage.toString());
        System.out.println("-------------------------");
    }
}