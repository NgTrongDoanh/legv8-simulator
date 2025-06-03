/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.util; 

import legv8.instructions.Instruction;

import java.util.BitSet;

/**
 * Extractor is a utility class that provides methods for extracting and extending bits from integers and BitSets.
 * It is used in the LEGv8 architecture to handle various instruction formats and bit manipulations.
 */
public class Extractor {
    /**
     * Private constructor to prevent instantiation of the utility class.
     */
    // This class should not be instantiated 
    private Extractor() {}

    /**
     * Extends the sign of an integer value based on the specified number of bits.
     * @param value The integer value to extend.
     * @param originalBits The number of bits in the original value (1-32).
     * @return The extended long value.
     * @throws IllegalArgumentException if originalBits is not in the range 1-32.
     */
    public static long extend(int value, int originalBits) {
        if (originalBits <= 0 || originalBits > 32) throw new IllegalArgumentException("originalBits for int input must be 1-32, was: " + originalBits);
        if (originalBits == 32) return value; 

        int signBitMask = 1 << (originalBits - 1);
        long lowerMask = (1L << originalBits) - 1;

        if ((value & signBitMask) != 0) { 
            long extensionMask = -1L << originalBits; 
            return ((long)value & lowerMask) | extensionMask; // Giữ các bit thấp và OR với mask mở rộng
        } else {     
            return (long)value & lowerMask; 
        }
    }

    /**
     * Extends the sign of a long value based on the specified number of bits.
     * @param value The long value to extend.
     * @param originalBits The number of bits in the original value (1-64).
     * @return The extended long value.
     * @throws IllegalArgumentException if originalBits is not in the range 1-64.
     */
    public static long extend(long value, int originalBits) {
        if (originalBits <= 0 || originalBits > 64) throw new IllegalArgumentException("originalBits for long input must be 1-64, was: " + originalBits);
        if (originalBits == 64) return value; 
        
        long signBitMask = 1L << (originalBits - 1);
        long lowerMask = (1L << originalBits) - 1;

        if ((value & signBitMask) != 0) { 
            long extensionMask = -1L << originalBits;
            return (value & lowerMask) | extensionMask;
        } else { 
            return value & lowerMask;
        }
    }

    /**
     * Extracts and extends a value from a BitSet based on the specified format and mnemonic.
     * @param instructionBits The BitSet representing the instruction.
     * @param format The format character (B, C, I, D, M, R).
     * @param mnemonic The mnemonic of the instruction.
     * @return The extended long value.
     * @throws IllegalArgumentException if the format is unsupported or if the instructionBits is null.
     */
    public static long extractAndExtend(BitSet instructionBits, char format, String mnemonic) {
        if (instructionBits == null) {
            throw new IllegalArgumentException("Instruction BitSet cannot be null.");
        }

        int rawValue = 0; 
        int numBits;  

        switch (format) {
            case 'B': 
                rawValue = Instruction.extractBits(instructionBits, 0, 25);
                numBits = 26;
                break;

            case 'C': 
                rawValue = Instruction.extractBits(instructionBits, 5, 23);
                numBits = 19;
                break;

            case 'I': 
                rawValue = Instruction.extractBits(instructionBits, 10, 21);
                numBits = 12;
                break;

            case 'D': 
                rawValue = Instruction.extractBits(instructionBits, 12, 20);
                numBits = 9;
                break;

            case 'M': 
                int hw = Instruction.extractBits(instructionBits, 21, 22); 
                int imm16 = Instruction.extractBits(instructionBits, 5, 20); 
                
                int shiftAmount = hw * 16;        
                long result = (long)imm16 << shiftAmount;
                return result;

            case 'R': 
                if (mnemonic.equals("LSL") || mnemonic.equals("LSR")) {
                    rawValue = Instruction.extractBits(instructionBits, 10, 15) & 0x3F; 
                    numBits = 6;
                }
                
                return rawValue;

            default:
                throw new IllegalArgumentException("Unsupported format for sign extension: " + format);
        }
        
        return extend(rawValue, numBits);
    }

    /**
     * Extracts and extends a value from an integer based on the specified format and mnemonic.
     * @param instruction The integer representing the instruction.
     * @param format The format character (B, C, I, D, M, R).
     * @param mnemonic The mnemonic of the instruction.
     * @return The extended long value.
     * @throws IllegalArgumentException if the format is unsupported.
     */
    public static long extractAndExtend(int instruction, char format, String mnemonic) {
        int rawValue = 0; 
        int numBits;  

        switch (format) {
            case 'B': 
                rawValue = instruction & 0x3FFFFFF; 
                numBits = 26;
                break;

            case 'C': // CB
                rawValue = (instruction >>> 5) & 0x7FFFF; 
                numBits = 19;
                break;

            case 'I': 
                rawValue = (instruction >>> 10) & 0xFFF; 
                numBits = 12;
                break;

            case 'D': 
                rawValue = (instruction >>> 12) & 0x1FF; 
                numBits = 9;
                break;

            case 'M': // IM
                int hw = (instruction >>> 21) & 0x3; 
                int imm16 = (instruction >>> 5) & 0xFFFF; 
                
                int shiftAmount = hw * 16;          
                long result = (long)imm16 << shiftAmount;
                return result;
                
            case 'R':
                if (mnemonic.equals("LSL") || mnemonic.equals("LSR") || mnemonic.equals("ASR")) {
                    rawValue = (instruction >>> 10) & 0x3F; 
                    numBits = 6;
                }
                
                return rawValue;
                
            default:
                throw new IllegalArgumentException("Unsupported format for sign extension: " + format);
        }

        return extend(rawValue, numBits);
    }
    // There maybe bugs in this code, be careful!
}