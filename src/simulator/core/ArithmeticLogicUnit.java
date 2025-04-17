package simulator.core;

import simulator.util.ALUOperation;
import simulator.util.ALUResult;

/**
 * Performs arithmetic and logic operations.
 * Calculates result and status flags (N, Z, C, V).
 */
public class ArithmeticLogicUnit {

    public ALUResult execute(long inputA, long inputB, ALUOperation operation) {
        long result = 0L;
        boolean nFlag = false, zFlag = false, cFlag = false, vFlag = false;
        int shiftAmount = (int)(inputB & 0x3F); // For shifts (mask to 0-63)

        switch (operation) {
            case ADD:
                result = inputA + inputB;
                // N = result < 0
                nFlag = (result < 0);
                // Z = result == 0
                zFlag = (result == 0L);
                // C = Unsigned carry out. True if unsigned sum < unsigned inputA (or inputB)
                cFlag = (Long.compareUnsigned(result, inputA) < 0);
                // V = Signed overflow. Occurs if inputs have same sign and result has different sign.
                // (A >= 0 && B >= 0 && Res < 0) || (A < 0 && B < 0 && Res >= 0)
                vFlag = ((inputA >= 0 && inputB >= 0 && result < 0) || (inputA < 0 && inputB < 0 && result >= 0));
                // Alt V Flag: ((inputA ^ inputB) < 0) is false (same sign) && ((inputA ^ result) < 0) is true (sign changed)
                // vFlag = ((inputA ^ inputB) >= 0) && ((inputA ^ result) < 0);
                break;
            case SUB:
                result = inputA - inputB;
                nFlag = (result < 0);
                zFlag = (result == 0L);
                // C = Not borrow. True if unsigned inputA >= unsigned inputB.
                cFlag = (Long.compareUnsigned(inputA, inputB) >= 0);
                // V = Signed overflow. Occurs if inputs have different signs and result has sign of inputB.
                // (A >= 0 && B < 0 && Res < 0) || (A < 0 && B >= 0 && Res >= 0)
                vFlag = ((inputA >= 0 && inputB < 0 && result < 0) || (inputA < 0 && inputB >= 0 && result >= 0));
                // Alt V Flag: ((inputA ^ inputB) < 0) is true (diff sign) && ((inputA ^ result) < 0) is true (sign matches B)
                // vFlag = ((inputA ^ inputB) < 0) && ((inputA ^ result) < 0);
                break;
            case AND:
                result = inputA & inputB;
                nFlag = (result < 0);
                zFlag = (result == 0L);
                cFlag = false; // Logical ops typically don't affect C, V
                vFlag = false;
                break;
            case ORR:
                result = inputA | inputB;
                nFlag = (result < 0);
                zFlag = (result == 0L);
                cFlag = false;
                vFlag = false;
                break;
            case EOR:
                result = inputA ^ inputB;
                nFlag = (result < 0);
                zFlag = (result == 0L);
                cFlag = false;
                vFlag = false;
                break;
            case LSL: // Logic Shift Left. Input B is shift amount.
                if (shiftAmount == 0) {
                    result = inputA;
                    cFlag = false; // No shift, no carry
                } else {
                    // Carry is the last bit shifted out (bit 64 - shiftAmount)
                    cFlag = ((inputA >>> (64 - shiftAmount)) & 1) == 1;
                    result = inputA << shiftAmount;
                }
                nFlag = (result < 0);
                zFlag = (result == 0L);
                vFlag = false; // Not defined for LSL
                break;
            case LSR: // Logic Shift Right (Unsigned). Input B is shift amount.
                 if (shiftAmount == 0) {
                    result = inputA;
                    cFlag = false;
                } else {
                    // Carry is the last bit shifted out (bit shiftAmount - 1)
                    cFlag = ((inputA >>> (shiftAmount - 1)) & 1) == 1;
                    result = inputA >>> shiftAmount; // Java >>> is logical shift right
                }
                // nFlag should always be false after >>> unless result is 0
                nFlag = false; // Technically should check result < 0, but >>> makes it 0 or positive
                zFlag = (result == 0L);
                vFlag = false;
                break;
            case ASR: // Arithmetic Shift Right (Signed). Input B is shift amount.
                 if (shiftAmount == 0) {
                    result = inputA;
                    cFlag = false;
                } else {
                    // Carry is the last bit shifted out (bit shiftAmount - 1)
                    cFlag = ((inputA >>> (shiftAmount - 1)) & 1) == 1; // Use logical shift to check bit
                    result = inputA >> shiftAmount; // Java >> is arithmetic shift right
                }
                nFlag = (result < 0);
                zFlag = (result == 0L);
                vFlag = false;
                break;
            case MOV: // Pass Input B (used for MOVZ/K immediate)
                result = inputB;
                nFlag = (result < 0);
                zFlag = (result == 0L);
                cFlag = false; // Usually unaffected
                vFlag = false;
                break;

            // --- Complex / Placeholder ---
            case MUL: // Implement basic multiply if needed, otherwise placeholder
            case SMULH:
            case UMULH:
            case SDIV:
            case UDIV:
                System.err.printf("ALU Warning: Operation %s not fully implemented. Returning 0.\n", operation);
                result = 0; // Placeholder
                nFlag = false; zFlag = true; cFlag = false; vFlag = false;
                break;

            // --- Idle/Unknown ---
             case IDLE:
                 // No operation, potentially return 0 or input A? Let's return 0.
                 result = 0;
                 nFlag = false; zFlag = true; cFlag = false; vFlag = false;
                 break;
             case UNKNOWN:
             default:
                 System.err.printf("ALU Error: Unknown operation %s. Returning 0.\n", operation);
                 result = 0;
                 nFlag = false; zFlag = true; cFlag = false; vFlag = false;
                 break;
        }

        return new ALUResult(result, nFlag, zFlag, cFlag, vFlag);
    }
}