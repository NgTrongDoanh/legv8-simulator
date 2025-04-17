package simulator.instructions;

import java.util.BitSet;

public class IMFormatInstruction extends Instruction {

    private final int rd, shiftAmount, immediate; // Raw 16-bit immediate

    public IMFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.shiftAmount = getShift_IM() * 16; // Calculate LSL amount (0, 16, 32, 48)
        this.immediate = getImmediate_IM();
        this.rd = getRd_IM();
    }

    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        // Immediate is treated as unsigned 16-bit value for display
        // Shift amount is displayed explicitly
        return String.format("%-6s X%d, #%d, LSL #%d", mnemonic, rd, immediate, shiftAmount);
    }
}