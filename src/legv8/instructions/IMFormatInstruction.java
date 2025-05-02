package legv8.instructions;

import java.util.BitSet;

public class IMFormatInstruction extends Instruction {

    private final int rd, shiftAmount, immediate; 

    public IMFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.shiftAmount = getShift_IM() * 16; 
        this.immediate = getImmediate_IM();
        this.rd = getRd_IM();
    }

    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        
        
        return String.format("%-6s X%d, #%d, LSL #%d", mnemonic, rd, immediate, shiftAmount);
    }
}