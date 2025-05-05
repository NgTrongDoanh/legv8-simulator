/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */
package legv8.instructions;

import legv8.util.ColoredLog;
import legv8.util.ControlSignals;

/**
 * InstructionDefinition is a class that represents the definition of an instruction in the LEGv8 architecture.
 * It contains information about the instruction's mnemonic, format, opcode identifier string, and control signals.
 */
public class InstructionDefinition {
    // --- Fields ---
    private final String mnemonic;
    private final char format; 
    private final String opcodeIdentifierString; 
    private final ControlSignals controlSignals;

    
    // --- Constructor ---
    
    /**
     * Constructor for InstructionDefinition.
     * @param mnemonic The mnemonic of the instruction.
     * @param format The format of the instruction (e.g., 'I', 'D', 'B', etc.).
     * @param opcodeIdentifierString The opcode identifier string in binary format.
     * @param controlSignals The control signals associated with the instruction.
     */
    public InstructionDefinition(String mnemonic, char format, String opcodeIdentifierString, ControlSignals controlSignals) {
        this.mnemonic = mnemonic;
        this.format = format;
        this.opcodeIdentifierString = opcodeIdentifierString;
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
     * @return The opcode identifier string in binary format.
     */
    public String getOpcodeIdentifierString() {
        return opcodeIdentifierString;
    }

    /**
     * @return The control signals associated with the instruction.
     */
    public ControlSignals getControlSignals() {
        return controlSignals;
    }

    /**
     * @return The opcode identifier value as an integer.
     *         This is obtained by parsing the opcode identifier string as a binary number.
     */
    public int getOpcodeIdentifierValue() {
        try {
            return Integer.parseInt(opcodeIdentifierString, 2);
        } catch (NumberFormatException e) {
            System.err.println(ColoredLog.ERROR + "Error parsing opcode identifier string '" + opcodeIdentifierString + "' for " + mnemonic);
            return -1;
        }
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
            mnemonic, format, opcodeIdentifierString, controlSignals
        );
    }
}
