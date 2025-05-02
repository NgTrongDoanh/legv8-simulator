package legv8.instructions;

import legv8.util.ColoredLog;
import legv8.util.ControlSignals;

public class InstructionDefinition {
    private final String mnemonic;
    private final char format; 
    private final String opcodeIdentifierString; 
    private final ControlSignals controlSignals;

    public InstructionDefinition(String mnemonic, char format, String opcodeIdentifierString, ControlSignals controlSignals) {
        this.mnemonic = mnemonic;
        this.format = format;
        this.opcodeIdentifierString = opcodeIdentifierString;
        this.controlSignals = controlSignals;
        
    }
    
    public String getMnemonic() {
        return mnemonic;
    }

    public char getFormat() {
        return format;
    }

    public String getOpcodeIdentifierString() {
        return opcodeIdentifierString;
    }

    public ControlSignals getControlSignals() {
        return controlSignals;
    }


    public int getOpcodeIdentifierValue() {
        try {
            return Integer.parseInt(opcodeIdentifierString, 2);
        } catch (NumberFormatException e) {
            System.err.println(ColoredLog.ERROR + "Error parsing opcode identifier string '" + opcodeIdentifierString + "' for " + mnemonic);
            return -1;
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "InstructionDefinition [mnemonic=%-6s, format=%c, opcodeIdentifierString=%-11s, controlSignals={%s}]",
            mnemonic, format, opcodeIdentifierString, controlSignals
        );
    }
}
