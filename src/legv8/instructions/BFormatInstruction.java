package legv8.instructions;

import java.util.BitSet;

import legv8.util.Extractor;

public class BFormatInstruction extends Instruction {

    private final int addressOffset; 

    public BFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.addressOffset = getAddress_B();
    }

    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        
        // Note: The value displayed is the *instruction* offset (target_addr - pc) / 4
        long signExtendedOffset = Extractor.extend(this.addressOffset, 26);
        
        
        return String.format("%-6s #%d", mnemonic, signExtendedOffset);
        
        
    }
}