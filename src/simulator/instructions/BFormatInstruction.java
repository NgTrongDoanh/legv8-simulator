package simulator.instructions;

import simulator.util.SignExtend;
import java.util.BitSet;

public class BFormatInstruction extends Instruction {

    private final int addressOffset; // Raw 26-bit address offset

    public BFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.addressOffset = getAddress_B();
    }

    @Override
    public String disassemble() {
        String mnemonic = definition.getMnemonic();
        // Sign-extend the 26-bit immediate offset for display
        // Note: The value displayed is the *instruction* offset (target_addr - pc) / 4
        long signExtendedOffset = SignExtend.extend(this.addressOffset, 26);
        // Multiply by 4 to show the byte offset, which is more common in disassembly?
        // Or show the raw instruction offset? Let's show instruction offset.
        return String.format("%-6s #%d", mnemonic, signExtendedOffset);
        // Alternative: Show byte offset relative to current instruction assumed address 0
        // return String.format("%-6s 0x%X ; (#%d)", mnemonic, signExtendedOffset * 4, signExtendedOffset);
    }
}