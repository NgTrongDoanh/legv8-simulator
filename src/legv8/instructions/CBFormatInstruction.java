/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.instructions;

import java.util.BitSet;
import legv8.util.Extractor;

/**
 * CBFormatInstruction is a class that represents a conditional branch format instruction in the LEGv8 architecture.
 * It extends the Instruction class and provides methods to disassemble the instruction and extract its components.
 */
public class CBFormatInstruction extends Instruction {
    private final int rt; // Register for CBZ/NZ, Condition code for B.cond
    private final int addressOffset; 

    // --- Constructor ---
    /**
     * Constructor for CBFormatInstruction.
     * @param bytecode The bytecode of the instruction as a BitSet.
     * @param definition The InstructionDefinition for this instruction.
     */
    public CBFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.addressOffset = getAddress_CB();
        this.rt = getRt_CB();
    }

    // --- Instruction Methods ---
    /**
     * @return The instruction as assembled string.
     *         The string is formatted as "mnemonic Xn, #offset" for CBZ/CBNZ
     *         or "mnemonic #offset" for B.cond.
     */
    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        long signExtendedOffset = Extractor.extend(this.addressOffset, 19);

        if (mnemonic.startsWith("B.")) { 
            return String.format("%-6s #%d", mnemonic, signExtendedOffset);
        } else { // CBZ/CBNZ - rt holds the register to test
            return String.format("%-6s X%d, #%d", mnemonic, rt, signExtendedOffset);
        }
    }
}