package simulator.instructions;

import simulator.util.SignExtend;
import java.util.BitSet;

public class IFormatInstruction extends Instruction {

    private final int rd, rn, immediate; // Raw 12-bit immediate

    public IFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.immediate = getImmediate_I();
        this.rn = getRn_I();
        this.rd = getRd_I();
    }

    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        // Sign-extend the 12-bit immediate for display
        long signExtendedImm = SignExtend.extend(this.immediate, 12);
        return String.format("%-6s X%d, X%d, #%d", mnemonic, rd, rn, signExtendedImm);
    }
}