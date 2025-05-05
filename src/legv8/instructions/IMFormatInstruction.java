/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.instructions;

import java.util.BitSet;

/**
 * IMFormatInstruction is a class that represents an immediate format instruction in the LEGv8 architecture.
 * It extends the Instruction class and provides methods to disassemble the instruction and extract its components.
 */
public class IMFormatInstruction extends Instruction {

    private final int rd, shiftAmount, immediate; 

    // --- Constructor ---
    /**
     * Constructor for IMFormatInstruction.
     * @param bytecode The bytecode of the instruction as a BitSet.
     * @param definition The InstructionDefinition for this instruction.
     */
    public IMFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.shiftAmount = getShift_IM() * 16; 
        this.immediate = getImmediate_IM();
        this.rd = getRd_IM();
    }

    // --- Instruction Methods ---
    /**
     * @return The instruction as assembled string.
     *         The string is formatted as "mnemonic Xn, #immediate, LSL #shiftAmount".
     */
    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        
        
        return String.format("%-6s X%d, #%d, LSL #%d", mnemonic, rd, immediate, shiftAmount);
    }
}