/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.instructions;

import java.util.BitSet;
import legv8.util.Extractor;

/**
 * BFormatInstruction is a class that represents a branch format instruction in the LEGv8 architecture.
 * It extends the Instruction class and provides methods to disassemble the instruction and extract its components.
 */
public class BFormatInstruction extends Instruction {
    // Address offset for the instruction
    private final int addressOffset; 

    // --- Constructor ---
    
    /**
     * Constructor for BFormatInstruction.
     * @param bytecode The bytecode of the instruction as a BitSet.
     * @param definition The InstructionDefinition for this instruction.
     */
    public BFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.addressOffset = getAddress_B();
    }

    
    // --- Instruction Methods ---

    /**
     * @return The instruction as assembled string.
     *         The string is formatted as "mnemonic #offset".
     */
    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        
        // Note: The value displayed is the *instruction* offset (target_addr - pc) / 4
        long signExtendedOffset = Extractor.extend(this.addressOffset, 26);
        
        
        return String.format("%-6s #%d", mnemonic, signExtendedOffset);
        
        
    }
}