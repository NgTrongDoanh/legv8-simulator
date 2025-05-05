/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.instructions;

import java.util.BitSet;

/**
 * DFormatInstruction is a class that represents a data format instruction in the LEGv8 architecture.
 * It extends the Instruction class and provides methods to disassemble the instruction and extract its components.
 */
public class DFormatInstruction extends Instruction {

    private final int rt, rn, address; 

    // --- Constructor ---
    /**
     * Constructor for DFormatInstruction.
     * @param bytecode The bytecode of the instruction as a BitSet.
     * @param definition The InstructionDefinition for this instruction.
     */
    public DFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.address = getAddress_D();
        this.rn = getRn_D();
        this.rt = getRt_D();
    }

    // --- Instruction Methods ---
    /**
     * @return The instruction as assembled string.
     *         The string is formatted as "mnemonic Xn, [Xm, #offset]".
     */
    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        return String.format("%-6s X%d, [X%d, #%d]", mnemonic, rt, rn, address);
    }
}