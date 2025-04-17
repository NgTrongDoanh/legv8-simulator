package simulator.instructions;

import simulator.util.SignExtend; // Import if needed for disassemble display
import java.util.BitSet;

public class RFormatInstruction extends Instruction {

    // Fields extracted for disassembly or direct access
    private final int rd, rn, rm, shamt;

    public RFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        // Extract fields specific to R-format using base class helper
        this.rm = getRm_R();
        this.shamt = getShamt_R();
        this.rn = getRn_R();
        this.rd = getRd_R();
    }

    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        // Format based on specific R-type mnemonic
        switch (mnemonic) {
            case "LSL", "LSR", "ASR":
                return String.format("%-6s X%d, X%d, #%d", mnemonic, rd, rn, shamt);
            case "BR": // Branch Register uses Rn field
                return String.format("%-6s X%d", mnemonic, rn);
            // Assume standard 3-register format for others
            case "ADD", "ADDS", "SUB", "SUBS", "AND", "ANDS", "ORR", "EOR":
            case "MUL", "SMULH", "UMULH", "SDIV", "UDIV": // Include complex ones if defined
                return String.format("%-6s X%d, X%d, X%d", mnemonic, rd, rn, rm);
            default:
                // Fallback for any other R-type not explicitly handled
                return String.format("%-6s X%d, X%d, X%d ; (shamt=%d)", mnemonic, rd, rn, rm, shamt);
        }
    }
}