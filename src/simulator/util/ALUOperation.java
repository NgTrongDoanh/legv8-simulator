package simulator.util;

/**
 * Defines the set of operations the ALU can perform.
 * (Giữ nguyên hoặc điều chỉnh từ code cũ của bạn)
 */
public enum ALUOperation {
    ADD, SUB, AND, ORR, EOR, LSL, LSR, ASR, MOV, // Các op cơ bản
    MUL, SMULH, UMULH, SDIV, UDIV, // Placeholder phức tạp
    PASS_A, // Ít dùng
    IDLE, UNKNOWN
}