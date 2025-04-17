package simulator.instructions;

import simulator.util.SignExtend;
import java.util.BitSet;

public class CBFormatInstruction extends Instruction {

    private final int rt; // Register for CBZ/NZ, Condition code for B.cond
    private final int addressOffset; // Raw 19-bit address offset

    public CBFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.addressOffset = getAddress_CB();
        this.rt = getRt_CB();
    }

    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        long signExtendedOffset = SignExtend.extend(this.addressOffset, 19);

        if (mnemonic.startsWith("B.")) { // B.cond - rt holds condition code
            return String.format("%-6s #%d", mnemonic, signExtendedOffset);
        } else { // CBZ/CBNZ - rt holds the register to test
            return String.format("%-6s X%d, #%d", mnemonic, rt, signExtendedOffset);
        }
    }
}