/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.util;

/**
 * FlagBranchControl is a utility class that provides methods to evaluate branch conditions based on CPU flags.
 * It contains a method to determine the result of a branch instruction based on the current state of the flags.
 */
public final class FlagBranchControl {
    // Helper class to encapsulate the result and message of the branch condition evaluation 
    public record FlagControl (
        boolean result,
        String message
    ) {}

    /**
     * Evaluates the branch condition based on the CPU flags and the condition code.
     * @param nFlag The negative flag (N).
     * @param zFlag The zero flag (Z).
     * @param cFlag The carry flag (C).
     * @param vFlag The overflow flag (V).
     * @param condCode The condition code to evaluate.
     * @return A FlagControl object containing the result and a message.
     */
    public static FlagControl getBranchCond(boolean nFlag, boolean zFlag, boolean cFlag, boolean vFlag, int condCode) {
        condCode &= 0xF; 
        switch (condCode) {
                case 0b0000: 
                    return new FlagControl(zFlag, "Z: " + zFlag);
                case 0b0001: 
                    return new FlagControl(!zFlag, "Z: " + zFlag);
                case 0b0010: 
                    return new FlagControl(cFlag, "C: " + cFlag);
                case 0b0011: 
                    return new FlagControl(!cFlag, "C: " + cFlag);
                case 0b0100: 
                    return new FlagControl(nFlag, "N: " + nFlag);
                case 0b0101: 
                    return new FlagControl(!nFlag, "N: " + nFlag);
                case 0b0110: 
                    return new FlagControl(vFlag, "V: " + vFlag);
                case 0b0111: 
                    return new FlagControl(!vFlag, "V: " + vFlag);
                case 0b1000: 
                    return new FlagControl(cFlag && !zFlag, "C: " + cFlag + ", Z: " + zFlag);
                case 0b1001: 
                    return new FlagControl(!cFlag || zFlag, "C: " + cFlag + ", Z: " + zFlag);
                case 0b1010: 
                    return new FlagControl(nFlag == vFlag, "N: " + nFlag + ", V: " + vFlag);
                case 0b1011: 
                    return new FlagControl(nFlag != vFlag, "N: " + nFlag + ", V: " + vFlag);
                case 0b1100: 
                    return new FlagControl(!zFlag && (nFlag == vFlag), "Z: " + zFlag + ", N: " + nFlag + ", V: " + vFlag);
                case 0b1101: 
                    return new FlagControl(zFlag || (nFlag != vFlag), "Z: " + zFlag + ", N: " + nFlag + ", V: " + vFlag);
                default: 
                    return new FlagControl(false, "Invalid condition code: " + condCode);
        }
    }
}
