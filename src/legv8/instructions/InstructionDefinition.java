/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */
package legv8.instructions;

import legv8.util.ControlSignals;

/**
 * InstructionDefinition is a class that represents the definition of an instruction in the LEGv8 architecture.
 * It contains information about the instruction's mnemonic, format, opcode identifier string, and control signals.
 */
public class InstructionDefinition {
    // --- Fields ---
    private final String mnemonic;
    private final char format;  // R: R-format, I: I-format, D: D-format, B: B-format, C: CB-format, M: IM-format
    private final int opcode; 
    private final ControlSignals controlSignals;

    
    // --- Constructor ---
    
    /**
     * Constructor for InstructionDefinition.
     * @param mnemonic The mnemonic of the instruction.
     * @param format The format of the instruction (R: R-format, I: I-format, D: D-format, B: B-format, C: CB-format, M: IM-format).
     * @param opcode The opcode of the instruction.
     * @param controlSignals The control signals associated with the instruction.
     */
    public InstructionDefinition(String mnemonic, char format, int opcode, ControlSignals controlSignals) {
        this.mnemonic = mnemonic;
        this.format = format;
        this.opcode = opcode;
        this.controlSignals = controlSignals;
    }

        /**
     * Constructor for InstructionDefinition.
     * @param mnemonic The mnemonic of the instruction.
     * @param format The format of the instruction (R: R-format, I: I-format, D: D-format, B: B-format, C: CB-format, M: IM-format).
     * @param opcodeIdentifierString The opcode identifier string in binary format.
     * @param controlSignals The control signals associated with the instruction.
     */
    public InstructionDefinition(String mnemonic, char format, String opcodeIdentifierString, ControlSignals controlSignals) {
        this.mnemonic = mnemonic;
        this.format = format;
        this.opcode = Integer.parseInt(opcodeIdentifierString, 2);
        this.controlSignals = controlSignals;    
    }
    

    // --- Getters ---

    /**
     * @return The mnemonic of the instruction.
     */
    public String getMnemonic() {
        return mnemonic;
    }

    /**
     * @return The format of the instruction.
     */
    public char getFormat() {
        return format;
    }

    /**
     * @return The opcode of the instruction as an integer.
     */
    public int getOpcode() {
        return opcode;
    }

    /**
     * @return The opcode identifier string in binary format.
     */
    public String getOpcodeIdentifierString() {
        return Integer.toBinaryString(opcode);
    }

    /**
     * @return The control signals associated with the instruction.
     */
    public ControlSignals getControlSignals() {
        return controlSignals;
    }
    
    // --- Utility Methods ---
    /**
     * @return A string representation of the InstructionDefinition object.
     *         This includes the mnemonic, format, opcode identifier string, and control signals.
     */
    @Override
    public String toString() {
        return String.format(
            "InstructionDefinition [mnemonic=%-6s, format=%c, opcodeIdentifierString=%-11s, controlSignals={%s}]",
            mnemonic, format, getOpcodeIdentifierString(), controlSignals
        );
    }
}
