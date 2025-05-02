package legv8.instructions;

import java.util.BitSet;

import legv8.util.Extractor;

public class IFormatInstruction extends Instruction {

    private final int rd, rn, immediate; 

    public IFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.immediate = getImmediate_I();
        this.rn = getRn_I();
        this.rd = getRd_I();
    }

    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        
        long signExtendedImm = Extractor.extend(this.immediate, 12);
        return String.format("%-6s X%d, X%d, #%d", mnemonic, rd, rn, signExtendedImm);
    }
}