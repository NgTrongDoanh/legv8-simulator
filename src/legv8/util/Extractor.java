package legv8.util; 

import java.util.BitSet;

import legv8.instructions.Instruction;


public class Extractor {

    
    private Extractor() {}

    public static long extend(int value, int originalBits) {
        if (originalBits <= 0 || originalBits > 32) {
            throw new IllegalArgumentException("originalBits for int input must be 1-32, was: " + originalBits);
        }
        if (originalBits == 32) {
            return value; 
        }

        int signBitMask = 1 << (originalBits - 1);

        long lowerMask = (1L << originalBits) - 1;
        if ((value & signBitMask) != 0) { 
            
            long extensionMask = -1L << originalBits; 
            
            return ((long)value & lowerMask) | extensionMask; // Giữ các bit thấp và OR với mask mở rộng
            
            
            
        } else { 
            
            return (long)value & lowerMask; 
        }
    }

    public static long extend(long value, int originalBits) {
        if (originalBits <= 0 || originalBits > 64) {
            throw new IllegalArgumentException("originalBits for long input must be 1-64, was: " + originalBits);
        }
        if (originalBits == 64) {
            return value; 
        }

        long signBitMask = 1L << (originalBits - 1);
        
        long lowerMask = (1L << originalBits) - 1;

        if ((value & signBitMask) != 0) { 
            
            long extensionMask = -1L << originalBits;
            
            return (value & lowerMask) | extensionMask;
        } else { 
            
            return value & lowerMask;
        }
    }

    public static long extractAndExtend(BitSet instructionBits, char format, String mnemonic) {
        if (instructionBits == null) {
             throw new IllegalArgumentException("Instruction BitSet cannot be null.");
        }
        if (instructionBits.length() > 32) {
            
            
        }

        int rawValue; 
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
                
                
                
                if (mnemonic.equals("LSL") || mnemonic.equals("LSR") || mnemonic.equals("ASR")) {
                    rawValue = Instruction.extractBits(instructionBits, 10, 1); 
                    numBits = 6;
                } else {
                    throw new IllegalArgumentException("Unsupported R-format instruction for sign extension: " + mnemonic);
                }
                
                return 0;
            default:
                throw new IllegalArgumentException("Unsupported format for sign extension: " + format);
        }

        
        return extend(rawValue, numBits);
    }

    public static long extractAndExtend(int instruction, char format, String mnemonic) {
        int rawValue; 
        int numBits;  

        switch (format) {
            case 'B': 
                
                rawValue = instruction & 0x3FFFFFF; 
                numBits = 26;
                break;
            case 'C': 
                
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
            case 'M':
                int hw = (instruction >>> 21) & 0x3; 
                int imm16 = (instruction >>> 5) & 0xFFFF; 
                
                int shiftAmount = hw * 16; 
                
                
                long result = (long)imm16 << shiftAmount;
                return result;
            case 'R':
                if (mnemonic.equals("LSL") || mnemonic.equals("LSR") || mnemonic.equals("ASR")) {
                    rawValue = (instruction >>> 10) & 0x3F; 
                    numBits = 6;
                } else {
                    return 0;
                }
                
            default:
                throw new IllegalArgumentException("Unsupported format for sign extension: " + format);
        }

        
        return extend(rawValue, numBits);
    }
}