package simulator.storage;

import java.util.Arrays;

public class RegisterStorage {
    public static final int NUM_REGISTERS = 32;
    public static final int ZERO_REGISTER_INDEX = 31; // XZR

    private final long[] registers;

    public RegisterStorage() {
        registers = new long[NUM_REGISTERS];
        // Mảng long mặc định là 0, đúng yêu cầu
        System.out.println("Register Storage initialized (" + NUM_REGISTERS + " registers).");
    }

    /** Gets value, ensuring XZR always reads 0. */
    public long getValue(int regNum) {
        validateRegisterNumber(regNum, "read");
        if (regNum == ZERO_REGISTER_INDEX) {
            return 0L;
        }
        return registers[regNum];
    }

    /**
     * Sets value directly. Bypasses XZR write protection (controller handles that).
     * Useful for initialization/testing.
     */
    public void setValue(int regNum, long value) {
        validateRegisterNumber(regNum, "write");
        // Không kiểm tra XZR ở đây, cho phép ghi vào XZR trong storage nếu cần (ví dụ test)
        // Controller sẽ ngăn chặn việc ghi vào XZR trong quá trình thực thi bình thường
        registers[regNum] = value;
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
        StringBuilder sb = new StringBuilder("Registers:\n");
        for (int i = 0; i < NUM_REGISTERS; i += 2) { // In 2 cột cho gọn
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