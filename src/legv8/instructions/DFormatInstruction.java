package legv8.instructions;

import java.util.BitSet;

public class DFormatInstruction extends Instruction {

    private final int rt, rn, address; 
    

    public DFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.address = getAddress_D();
        this.rn = getRn_D();
        this.rt = getRt_D();
        
    }

    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        
        return String.format("%-6s X%d, [X%d, #%d]", mnemonic, rt, rn, address);
    }
}