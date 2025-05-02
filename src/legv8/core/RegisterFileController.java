package legv8.core;

import java.util.Objects;

import legv8.storage.RegisterStorage;
import legv8.util.ColoredLog;

public class RegisterFileController {
    private final RegisterStorage storage;

    public RegisterFileController(RegisterStorage storage) {
        this.storage = Objects.requireNonNull(storage, ColoredLog.WARNING + "RegisterStorage cannot be null.");
        System.out.println(ColoredLog.SUCCESS + "Register File Controller initialized.");
    }

    
    public long readRegister(int readReg) {
        return storage.getValue(readReg);
    }

    public void writeRegister(int writeReg, long writeData, boolean regWrite) {
        // writeData = writeData & RegisterStorage.VALUE_MASK; 
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

    
    public RegisterStorage getStorage() {
        return new RegisterStorage(storage);
    }

    public void clearStorage() {
        storage.clear();
    }

    
    public void displayStorage() {
        System.out.println(ColoredLog.INFO + "Register File State:");
        System.out.println("--- Registers Content ---");
        System.out.println(storage.toString());
        System.out.println("-------------------------");
    }
}