package legv8.storage;

import java.util.Arrays;

import legv8.util.ColoredLog;

public class RegisterStorage {
    public static final int NUM_REGISTERS = 32;
    public static final int STACK_POINTER_INDEX = 28; 
    public static final int FRAME_POINTER_INDEX = 29; 
    public static final int LINK_REGISTER_INDEX = 30; 
    public static final int ZERO_REGISTER_INDEX = 31; 

    public static final long VALUE_MASK = 0xFFFFFFFFFFFFFFFFL; // Sửa thành 64-bit
    private final long[] registers;

    public RegisterStorage() {
        registers = new long[NUM_REGISTERS];
        
        System.out.println(ColoredLog.SUCCESS + "Register Storage initialized (" + NUM_REGISTERS + " registers).");
    }

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

    public long getValue(int regNum) {
        validateRegisterNumber(regNum, "read");
        if (regNum == ZERO_REGISTER_INDEX) {
            return 0L;
        }
        return registers[regNum];
    }

    public void setValue(int regNum, long value) {
        value = value & VALUE_MASK; 
        validateRegisterNumber(regNum, "write");
        
        if (regNum == ZERO_REGISTER_INDEX) {
            
            System.out.println(ColoredLog.WARNING + "RegisterStorage Info: Ignored write to XZR.");
            return; 
        }
        
        registers[regNum] = value;
        
        System.err.printf("%sRegisterStorage Write: X%d <= 0x%016X\n", ColoredLog.INFO, regNum, value);
    }

    public void clear() {
        Arrays.fill(registers, 0L);
    }

    private void validateRegisterNumber(int regNum, String operation) {
        if (regNum < 0 || regNum >= NUM_REGISTERS) {
            throw new IllegalArgumentException("Invalid register number for " + operation + ": " + regNum + ". Must be 0-" + (NUM_REGISTERS - 1));
        }
    }

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