/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.instructions;

import java.util.BitSet;
import legv8.util.Extractor;

/**
 * IFormatInstruction is a class that represents an immediate format instruction in the LEGv8 architecture.
 * It extends the Instruction class and provides methods to disassemble the instruction and extract its components.
 */
public class IFormatInstruction extends Instruction {

    private final int rd, rn, immediate; 

    // --- Constructor ---
    /**
     * Constructor for IFormatInstruction.
     * @param bytecode The bytecode of the instruction as a BitSet.
     * @param definition The InstructionDefinition for this instruction.
     */
    public IFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.immediate = getImmediate_I();
        this.rn = getRn_I();
        this.rd = getRd_I();
    }

    // --- Instruction Methods ---
    /**
     * @return The instruction as assembled string.
     *         The string is formatted as "mnemonic Xn, Xn, #immediate".
     */
    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        
        long signExtendedImm = Extractor.extend(this.immediate, 12);
        return String.format("%-6s X%d, X%d, #%d", mnemonic, rd, rn, signExtendedImm);
    }
}