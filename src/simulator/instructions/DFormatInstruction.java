package simulator.instructions;

// No need for SignExtend here as offset is typically shown unsigned
import java.util.BitSet;

public class DFormatInstruction extends Instruction {

    private final int rt, rn, address; // Raw 9-bit address offset

    public DFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.address = getAddress_D();
        this.rn = getRn_D();
        this.rt = getRt_D();
        // op2 field is ignored for LDUR/STUR
    }

    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        // D-format offset is usually displayed as an unsigned immediate
        return String.format("%-6s X%d, [X%d, #%d]", mnemonic, rt, rn, address);
    }
}