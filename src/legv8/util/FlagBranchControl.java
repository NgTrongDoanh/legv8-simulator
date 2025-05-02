package legv8.util;

import legv8.instructions.InstructionConfigLoader;
import java.util.Objects;


public final class FlagBranchControl {
    
    public record FlagControl (
        boolean result,
        String message
    ) {}

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
