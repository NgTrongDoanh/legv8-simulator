/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.core;

import legv8.util.ALUResult;

/**
 * Simulates the Arithmetic Logic Unit (ALU) of the LEGv8 CPU.
 * Performs various 64-bit arithmetic (ADD, SUB, MUL, DIV) and logical
 * (AND, ORR, EOR) operations, as well as shift operations (LSL, LSR, ASR).
 * It takes two 64-bit inputs (A and B) and an operation code, and produces
 * a 64-bit result along with the status flags (N, Z, C, V).
 */
public class ArithmeticLogicUnit {

    // --- ALU Operation Codes ---
    // These codes define the specific operation the ALU should perform.
    // They should match the 'operation' field values defined in the instruction config.
    public static final int AND    = 0b0000;  
    public static final int OR     = 0b0001;  
    public static final int ADD    = 0b0010;  
    public static final int XOR    = 0b0011;
    // public static final int UDIV    = 0b0100; // Not used.
    // public static final int SDIV    = 0b0101; // Not used.
    public static final int SUB    = 0b0110;  
    public static final int PASS_B = 0b0111;  
    public static final int LSL    = 0b1000;  
    public static final int LSR    = 0b1001;  
    // public static final int ASR    = 0b1010;  
    // public static final int MUL    = 0b1011;  
    // public static final int SMULH  = 0b1100;  
    // public static final int UMULH  = 0b1101;  
    public static final int MOVK  = 0b1110;
    public static final int MOVZ  = 0b1111;

    public static final int IDLE   = 404;  


    // --- Constructor ---

    /**
     * Constructs a new ArithmeticLogicUnit instance.
     * (No internal state needs initialization).
     */
    public ArithmeticLogicUnit() {
        // Optional log: System.out.println(ColoredLog.SUCCESS + "Arithmetic Logic Unit initialized.");
    }

    
    // --- Public Execution Method ---

    /**
     * Executes the specified ALU operation on the given 64-bit inputs.
     * Calculates the 64-bit result and the state of the N, Z, C, and V flags.
     * For shift operations, the lower 6 bits of {@code inputB} are used as the shift amount.
     *
     * @param inputA The first 64-bit operand (e.g., from Register File Read Data 1).
     * @param inputB The second 64-bit operand (e.g., from Register File Read Data 2 or sign-extended immediate).
     * @param operation The integer code specifying the ALU operation to perform (e.g., {@code ALU.ADD}).
     * @return An {@link ALUResult} object containing the 64-bit result and the boolean values of the N, Z, C, V flags.
     */
    public ALUResult execute(long inputA, long inputB, int operation) {
        long result = 0L;
        boolean nFlag = false, zFlag = false, cFlag = false, vFlag = false;

        switch (operation) {
            case AND:
                result = inputA & inputB;
                break;

            case OR:
                result = inputA | inputB;
                break;

            case ADD:
                result = inputA + inputB;
                cFlag = Long.compareUnsigned(result, inputA) < 0; 
                vFlag = ((inputA ^ ~inputB) & (inputA ^ result)) < 0;
                break;

            case XOR:
                result = inputA ^ inputB;
                break;

            case SUB:
                result = inputA - inputB;
                cFlag = Long.compareUnsigned(inputA, inputB) >= 0; 
                vFlag = ((inputA ^ inputB) & (inputA ^ result)) < 0;
                break;

            case PASS_B:
                result = inputB;
                break;

            case LSL:
                result = inputA << inputB;
                break;

            case LSR:
                result = inputA >>> inputB;
                break;

            case MOVZ:
                // result = inputA << ((int) inputB * 16);
                result = inputB;
                break;

            case MOVK: 
                // int shift = (int) inputB * 16;
                // long mask = ~(0xFFFFL << shift);
                // result = (inputA & 0xFFFFL) << shift | (inputB & mask);
                result = inputB;
                break;
            
            case IDLE:
            default:
                result = 0L; // Default case, no operation performed.
                break;
        }

        nFlag = result < 0; 
        zFlag = result == 0;
        
        if (!(operation == ADD || operation == SUB)) {
            cFlag = false;
            vFlag = false;
        }

        return new ALUResult(result, nFlag, zFlag, cFlag, vFlag);
    }
}