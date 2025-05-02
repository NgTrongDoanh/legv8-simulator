package legv8.instructions;

import java.util.BitSet;

public class RFormatInstruction extends Instruction {
    private final int rd, rn, rm, shamt;

    public RFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.rm = getRm_R();
        this.shamt = getShamt_R();
        this.rn = getRn_R();
        this.rd = getRd_R();
    }

    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        
        switch (mnemonic) {
            case "LSL", "LSR", "ASR":
                return String.format("%-6s X%d, X%d, #%d", mnemonic, rd, rn, shamt);

            case "BR": 
                return String.format("%-6s X%d", mnemonic, rn);

            
            case "ADD", "ADDS", "SUB", "SUBS", "AND", "ANDS", "ORR", "EOR":
            case "MUL", "SMULH", "UMULH", "SDIV", "UDIV": 
                return String.format("%-6s X%d, X%d, X%d", mnemonic, rd, rn, rm);

            default:
                
                return String.format("%-6s X%d, X%d, X%d ; (shamt=%d)", mnemonic, rd, rn, rm, shamt);
        }
    }

}
