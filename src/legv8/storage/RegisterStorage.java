/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.storage;

import legv8.util.ColoredLog;

import java.util.Arrays;

/**
 * RegisterStorage is a class that represents the storage for registers in the LEGv8 architecture.
 * It provides methods to get and set register values, clear the storage, and validate register numbers.
 */
public class RegisterStorage {
    // --- Constants ---
    // Number of registers in LEGv8 architecture
    public static final int NUM_REGISTERS = 32;

    // SP: Stack Pointer, FP: Frame Pointer, LR: Link Register, XZR: Zero Register
    public static final int STACK_POINTER_INDEX = 28; 
    public static final int FRAME_POINTER_INDEX = 29; 
    public static final int LINK_REGISTER_INDEX = 30; 
    public static final int ZERO_REGISTER_INDEX = 31; 

    // Mask for 64-bit value
    public static final long VALUE_MASK = 0xFFFFFFFFFFFFFFFFL; // Sửa thành 64-bit

    // Register storage array
    // The array to store register values
    private final long[] registers;


    // --- Constructor ---

    /**
     * Constructor for RegisterStorage.
     * Initializes the register storage with the default size.
     */
    public RegisterStorage() {
        registers = new long[NUM_REGISTERS];
        System.out.println(ColoredLog.SUCCESS + "Register Storage initialized (" + NUM_REGISTERS + " registers).");
    }

    /**
     * Copy constructor for RegisterStorage.
     * Initializes the register storage with the values from another RegisterStorage object.
     * @param other The RegisterStorage object to copy from.
     * @throws IllegalArgumentException if the other RegisterStorage is null or has an invalid size.
     * @throws ArrayIndexOutOfBoundsException if the register number is out of bounds.
     */
    public RegisterStorage(RegisterStorage other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot copy from null RegisterStorage.");
        }
        if (other.registers.length != NUM_REGISTERS) {
            throw new IllegalArgumentException("Invalid RegisterStorage size: " + other.registers.length);
        }

        registers = new long[NUM_REGISTERS];
        System.arraycopy(other.registers, 0, this.registers, 0, NUM_REGISTERS);
    }

    // --- Register Access Methods ---
    /**
     * Gets the value of a register.
     * @param regNum The register number (0-31).
     * @return The value of the register.
     * @throws IllegalArgumentException if the register number is invalid.
     */
    public long getValue(int regNum) {
        validateRegisterNumber(regNum, "read");
        if (regNum == ZERO_REGISTER_INDEX) {
            return 0L;
        }
        return registers[regNum];
    }

    /**
     * Sets the value of a register.
     * @param regNum The register number (0-31).
     * @param value The value to set in the register.
     * @throws IllegalArgumentException if the register number is invalid.
     */
    public void setValue(int regNum, long value) {
        value = value & VALUE_MASK; 
        validateRegisterNumber(regNum, "write");
        
        if (regNum == ZERO_REGISTER_INDEX) {     
            System.out.println(ColoredLog.WARNING + "RegisterStorage Info: Ignored write to XZR.");
            return; 
        }
        registers[regNum] = value;
        
        // Log the write operation
        System.err.printf("%sRegisterStorage Write: X%d <= 0x%016X\n", ColoredLog.INFO, regNum, value);
    }

    /**
     * Clears the values of all registers.
     * Sets all registers to 0.
     */
    public void clear() {
        Arrays.fill(registers, 0L);
    }

    /**
     * Validates the register number.
     * @param regNum The register number to validate.
     * @param operation The operation being performed (read/write).
     * @throws IllegalArgumentException if the register number is invalid.
     */
    private void validateRegisterNumber(int regNum, String operation) {
        if (regNum < 0 || regNum >= NUM_REGISTERS) {
            throw new IllegalArgumentException("Invalid register number for " + operation + ": " + regNum + ". Must be 0-" + (NUM_REGISTERS - 1));
        }
    }


    // --- Utility Methods ---
    /**
     * Returns a string representation of the register storage.
     * @return A string representation of the register storage.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < NUM_REGISTERS; i += 2) { 
            String regName1 = (i == ZERO_REGISTER_INDEX) ? "XZR" : ("X" + i);
            String regName2 = ((i + 1) == ZERO_REGISTER_INDEX) ? "XZR" : ("X" + (i + 1));
            if (i == 28) regName1 = "SP";
            if ((i+1) == 28) regName2 = "SP";

            sb.append(String.format(" %-3s(%-2d): %016X   ", regName1, i, registers[i]));
            if (i + 1 < NUM_REGISTERS) {
                sb.append(String.format("%-3s(%-2d): %016X\n", regName2, i + 1, registers[i+1]));
            } else {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}