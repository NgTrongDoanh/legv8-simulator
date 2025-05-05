/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.instructions;

import legv8.util.ColoredLog;
import java.util.BitSet;
import java.util.Objects;

/**
 * Instruction is an abstract class that represents a generic instruction in the LEGv8 architecture.
 * It provides methods to manipulate and extract information from the instruction's bytecode.
 */
// Note: This class is not intended to be instantiated directly.
// Instead, it should be extended by specific instruction classes (e.g., RFormatInstruction, IFormatInstruction, etc.).
public abstract class Instruction {
    // --- Fields ---
    protected final BitSet bytecode;
    protected final InstructionDefinition definition;

    
    // --- Constructor ---

    /**
     * Constructor for the Instruction class.
     * @param bytecode The bytecode of the instruction as a BitSet.
     * @param definition The InstructionDefinition for this instruction.
     */
    protected Instruction(BitSet bytecode, InstructionDefinition definition) { 
        if (bytecode.length() > 32) {
            System.err.printf("%sWarning: Bytecode provided to Instruction constructor has length %d (> 32) for %s.\n",
                                ColoredLog.WARNING, bytecode.length(), definition.getMnemonic());
        }
        
        this.bytecode = (BitSet) Objects.requireNonNull(bytecode, ColoredLog.WARNING + "Bytecode cannot be null.").clone();
        this.definition = Objects.requireNonNull(definition, ColoredLog.WARNING + "InstructionDefinition cannot be null for standard instruction creation.");
    }

    
    // --- Getters ---
    
    /**
     * @return The bytecode of the instruction as a BitSet.
     *         The clone is used to prevent external modification of the original bytecode.
     */
    public BitSet getBytecode() {
        return (BitSet) bytecode.clone(); 
    }

    /**
     * @return The InstructionDefinition for this instruction.
     */
    // Note: This method is not intended to be used for instruction creation.
    public InstructionDefinition getDefinition() {
        return definition;
    }

    // --- Bit Extraction Methods ---
    // R-Format Instruction
    public int getOpcode_R()    { return extractBits(bytecode, 21, 31); } 
    public int getRm_R()        { return extractBits(bytecode, 16, 20); } 
    public int getShamt_R()     { return extractBits(bytecode, 10, 15); } 
    public int getRn_R()        { return extractBits(bytecode, 5, 9); }   
    public int getRd_R()        { return extractBits(bytecode, 0, 4); }   

    // I-Format Instruction
    public int getOpcode_I()    { return extractBits(bytecode, 22, 31); } 
    public int getImmediate_I() { return extractBits(bytecode, 10, 21); } 
    public int getRn_I()        { return extractBits(bytecode, 5, 9); }   
    public int getRd_I()        { return extractBits(bytecode, 0, 4); }   

    // D-Format Instruction
    public int getOpcode_D()    { return extractBits(bytecode, 21, 31); } 
    public int getAddress_D()   { return extractBits(bytecode, 12, 20); } 
    public int getOp2_D()       { return extractBits(bytecode, 10, 11); } // 2 bits (Unused in LDUR/STUR)
    public int getRn_D()        { return extractBits(bytecode, 5, 9); }   
    public int getRt_D()        { return extractBits(bytecode, 0, 4); }   

    // B-Format Instruction
    public int getOpcode_B()    { return extractBits(bytecode, 26, 31); } 
    public int getAddress_B()   { return extractBits(bytecode, 0, 25); }  

    // CB-Format Instruction
    public int getOpcode_CB()   { return extractBits(bytecode, 24, 31); } 
    public int getAddress_CB()  { return extractBits(bytecode, 5, 23); } 
    public int getRt_CB()       { return extractBits(bytecode, 0, 4); }  // 5 bits (Rt for CBZ/NZ, Cond for B.cond)

    // I-Format Immediate Instruction
    public int getOpcode_IM()   { return extractBits(bytecode, 23, 31); } 
    public int getShift_IM()    { return extractBits(bytecode, 21, 22); } 
    public int getImmediate_IM(){ return extractBits(bytecode, 5, 20); } 
    public int getRd_IM()       { return extractBits(bytecode, 0, 4); }   


    // --- Bit Manipulation Methods ---

    /**
     * Sets the bits in the specified range of the BitSet to the value provided.
     * @param bits The BitSet to modify.
     * @param value The value to set in the specified bit range.
     * @param startBit The starting bit index (inclusive).
     * @param endBit The ending bit index (inclusive).
     */
    protected static void setBits(BitSet bits, int value, int startBit, int endBit) {
        if (startBit < 0 || endBit < startBit || endBit >= 32) {
            System.err.printf("%sWarning: Invalid bit range for setting: %d to %d\n", ColoredLog.WARNING, startBit, endBit);
            return; 
        }

        int mask = 1;
        for (int i = startBit; i <= endBit; i++) {
            bits.set(i, (value & mask) != 0);
            mask <<= 1;
        }
    }
    
    /**
     * Extracts a range of bits from the BitSet and returns it as an integer.
     * @param bits The BitSet to extract bits from.
     * @param startBit The starting bit index (inclusive).
     * @param endBit The ending bit index (inclusive).
     * @return The extracted value as an integer.
     */
    public static int extractBits(BitSet bits, int startBit, int endBit) {
        if (startBit < 0 || endBit < startBit || endBit >= 32) {      
            System.err.printf("%sWarning: Invalid bit range requested: %d to %d\n", ColoredLog.WARNING, startBit, endBit);    
            throw new IllegalArgumentException("Invalid bit range requested: " + startBit + " to " + endBit);
        }

        int value = 0;
        for (int i = startBit; i <= endBit; i++) {
            if (bits.get(i)) {
                value |= (1 << (i - startBit));
            }
        }
        
        return value;
    }

    /**
     * Formats the BitSet as a binary string.
     * @param bits The BitSet to format.
     * @return The formatted binary string.
     */    
    public static String formatBitSet(BitSet bits) {
        StringBuilder sb = new StringBuilder(35); 
        for (int i = 31; i >= 0; i--) {
            sb.append(bits.get(i) ? '1' : '0');
            if (i > 0 && i % 8 == 0) {
                sb.append(' '); 
            }
        }
        return sb.toString();
    }


    // --- Utility Methods ---
    
    /**
     * Abstract method to disassemble the instruction into a human-readable format.
     * @return The disassembled instruction as a string.
     */
    public abstract String disassemble();

    /**
     * @return A string representation of the instruction, including its bytecode in binary format.
     */
    @Override
    public String toString() {
        return disassemble() + "\n" + formatBitSet(bytecode);
    }

} 
