package simulator.instructions;

import simulator.util.ALUOperation;
import simulator.util.ControlSignals;
// Không cần import BitSet ở đây

/**
 * Holds the static definition of an instruction type, including its
 * format, mnemonic, control signals, and ALU operation.
 * Loaded from the configuration file.
 */
public class InstructionDefinition {
    private final String mnemonic;
    private final char format; // 'R', 'I', 'D', 'B', 'C' (CB), 'M' (IM)
    private final String opcodeIdentifierString; // Binary string used for matching in config
    private final ControlSignals controlSignals;
    private final ALUOperation aluOperation; // Operation the ALU should perform for this instruction

    public InstructionDefinition(String mnemonic, char format, String opcodeIdentifierString,
                                 ControlSignals controlSignals, ALUOperation aluOperation) {
        this.mnemonic = mnemonic;
        this.format = format;
        this.opcodeIdentifierString = opcodeIdentifierString;
        this.controlSignals = controlSignals;
        this.aluOperation = aluOperation;
    }

    // Getters
    public String getMnemonic() { return mnemonic; }
    public char getFormat() { return format; }
    public String getOpcodeIdentifierString() { return opcodeIdentifierString; }
    public ControlSignals getControlSignals() { return controlSignals; }
    public ALUOperation getAluOperation() { return aluOperation; }

    /**
     * Gets the integer value of the opcode identifier string.
     * Used for map lookups. Returns -1 on parsing error.
     */
    public int getOpcodeIdentifierValue() {
        try {
            // Ensure parsing as unsigned binary if needed, but parseInt handles positive values correctly
            return Integer.parseInt(opcodeIdentifierString, 2);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing opcode identifier string '" + opcodeIdentifierString + "' for " + mnemonic);
            return -1;
        }
    }

    @Override
    public String toString() {
        return String.format("Def[Mnem=%-6s Fmt=%c OpId=%-11s ALUOp=%-5s Signals={%s}]",
                             mnemonic, format, opcodeIdentifierString, aluOperation, controlSignals);
    }
}