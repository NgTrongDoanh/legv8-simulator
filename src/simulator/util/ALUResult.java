package simulator.util;

/**
 * Holds the result of an ALU operation (value and flags).
 * (Giữ nguyên hoặc điều chỉnh từ code cũ của bạn)
 */
public record ALUResult(
    long result,
    boolean negativeFlag, // N
    boolean zeroFlag,     // Z
    boolean carryFlag,    // C
    boolean overflowFlag  // V
) {
    @Override
    public String toString() {
        return String.format("Res=0x%016X (N=%b Z=%b C=%b V=%b)",
                             result, negativeFlag, zeroFlag, carryFlag, overflowFlag);
    }
}