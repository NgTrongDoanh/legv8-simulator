package legv8.core;

import legv8.util.ALUResult;
import legv8.util.ColoredLog;

public class ArithmeticLogicUnit {
    
    public static final int ADD    = 0b0010;  
    public static final int SUB    = 0b0110;  
    public static final int AND    = 0b0000;  
    public static final int ORR    = 0b0001;  
    public static final int EOR    = 0b0011;  
    public static final int LSL    = 0b1000;  
    public static final int LSR    = 0b1001;  
    public static final int ASR    = 0b1010;  
    public static final int MUL    = 0b1100;  
    public static final int SMULH  = 0b1101;  
    public static final int UMULH  = 0b1110;  
    public static final int SDIV   = 0b1111;  
    public static final int UDIV   = 0b1011;  
    public static final int PASS_B = 0b0111;  

    public static final int IDLE   = 404;  

    public ALUResult execute(long inputA, long inputB, int operation) {
        long result = 0L;
        boolean nFlag = false, zFlag = false, cFlag = false, vFlag = false;
        int shiftAmount = (int)(inputB & 0x3F); 

        switch (operation) {
            case ADD:
                result = inputA + inputB;
                nFlag = (result < 0);
                zFlag = (result == 0L);
                cFlag = (Long.compareUnsigned(result, inputA) < 0); 
                vFlag = ((inputA >= 0 && inputB >= 0 && result < 0) || (inputA < 0 && inputB < 0 && result >= 0)); 

                break;

            case SUB:
                result = inputA - inputB;
                nFlag = (result < 0);
                zFlag = (result == 0L);
                cFlag = (Long.compareUnsigned(inputA, inputB) >= 0); 
                vFlag = ((inputA >= 0 && inputB < 0 && result < 0) || (inputA < 0 && inputB >= 0 && result >= 0)); 
                
                break;

            case AND:
                result = inputA & inputB;
                nFlag = (result < 0);
                zFlag = (result == 0L);
                cFlag = false; 
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

            case LSL:
                if (shiftAmount == 0) {
                    result = inputA;
                    cFlag = false;
                } else {
                    cFlag = ((inputA >>> (64 - shiftAmount)) & 1) == 1; 
                    result = inputA << shiftAmount;
                }
                nFlag = (result < 0);
                zFlag = (result == 0L);
                vFlag = false;
                
                break;

            case LSR:
                if (shiftAmount == 0) {
                    result = inputA;
                    cFlag = false;
                } else {
                    cFlag = ((inputA >>> (shiftAmount - 1)) & 1) == 1; 
                    result = inputA >>> shiftAmount; 
                }

                nFlag = false; 
                zFlag = (result == 0L);
                vFlag = false;
                
                break;

            case ASR:
                if (shiftAmount == 0) {
                    result = inputA;
                    cFlag = false;
                } else {
                    cFlag = ((inputA >> (shiftAmount - 1)) & 1) == 1; 
                    result = inputA >> shiftAmount; 
                }
                
                nFlag = (result < 0);
                zFlag = (result == 0L);
                vFlag = false;
                
                break;

            case MUL:
                result = inputA * inputB; 
                nFlag = (result < 0);
                zFlag = (result == 0L);
                cFlag = false; 
                vFlag = false; 
                
                break;

            case SMULH:
                java.math.BigInteger a = java.math.BigInteger.valueOf(inputA);
                java.math.BigInteger b = java.math.BigInteger.valueOf(inputB);
                java.math.BigInteger product = a.multiply(b); 
                result = product.shiftRight(64).longValue(); 
                nFlag = (result < 0);
                zFlag = (result == 0L);
                cFlag = false;
                vFlag = false;
                
                break;

            case UMULH:
                java.math.BigInteger ua = new java.math.BigInteger(1, java.nio.ByteBuffer.allocate(8).putLong(inputA).array());
                java.math.BigInteger ub = new java.math.BigInteger(1, java.nio.ByteBuffer.allocate(8).putLong(inputB).array());
                java.math.BigInteger uproduct = ua.multiply(ub); 
                result = uproduct.shiftRight(64).longValue(); 
                nFlag = (result < 0);
                zFlag = (result == 0L);
                cFlag = false;
                vFlag = false;
                
                break;

            case SDIV:
                if (inputB == 0) result = 0;  
                else result = inputA / inputB;

                nFlag = (result < 0);
                zFlag = (result == 0L);
                cFlag = false;
                vFlag = false;
                
                break;

            case UDIV:
                if (inputB == 0) result = 0; 
                else result = Long.divideUnsigned(inputA, inputB);
        
                nFlag = false; 
                zFlag = (result == 0L);
                cFlag = false;
                vFlag = false;

                break;

            case PASS_B:
                result = inputB; 
                nFlag = (result < 0);
                zFlag = (result == 0L);
                cFlag = false;
                vFlag = false;
                
                break;

            default:
                System.err.printf("%sALU Error: Unknown operation %d. Returning 0.\n", ColoredLog.FAILURE, operation);
                
                result = 0;
                nFlag = false;
                zFlag = true;
                cFlag = false;
                vFlag = false;
                
                break;
        }

        return new ALUResult(result, nFlag, zFlag, cFlag, vFlag);
    }
}