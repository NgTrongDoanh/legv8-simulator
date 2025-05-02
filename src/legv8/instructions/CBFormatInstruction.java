package legv8.instructions;

import java.util.BitSet;

import legv8.util.Extractor;

public class CBFormatInstruction extends Instruction {

    private final int rt; // Register for CBZ/NZ, Condition code for B.cond
    private final int addressOffset; 

    public CBFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.addressOffset = getAddress_CB();
        this.rt = getRt_CB();
    }

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