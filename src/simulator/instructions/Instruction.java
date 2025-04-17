package simulator.instructions;

import java.util.BitSet;
import java.util.Objects;

/**
 * Abstract base class for all LEGv8 instructions.
 * Holds the bytecode and the instruction definition.
 * Provides helper methods for extracting bit fields and encoding (though encoding might be static).
 */
public abstract class Instruction {
    protected final BitSet bytecode;
    protected final InstructionDefinition definition;

    /**
     * Constructs an Instruction.
     * @param bytecode The 32-bit machine code. Must not be null.
     * @param definition The corresponding definition. Should not be null for valid instructions.
     */
    protected Instruction(BitSet bytecode, InstructionDefinition definition) {
        Objects.requireNonNull(bytecode, "Bytecode cannot be null.");
        // Allow null definition for potentially unrecognized instructions during direct bytecode decode?
        // Or throw here? Let's require definition for now.
        Objects.requireNonNull(definition, "InstructionDefinition cannot be null for standard instruction creation.");

        if (bytecode.length() > 32) {
             // Trim or warn/error if bytecode is longer than 32 bits?
             // Let's just clone what we get for now.
             System.err.printf("Warning: Bytecode provided to Instruction constructor has length %d (> 32) for %s.\n",
                              bytecode.length(), definition.getMnemonic());
        }
        this.bytecode = (BitSet) bytecode.clone(); // Defensive copy
        this.definition = definition;
    }

    /** Returns the machine code representation. */
    public BitSet getBytecode() {
        return (BitSet) bytecode.clone(); // Return a copy
    }

     /** Returns the associated InstructionDefinition. */
    public InstructionDefinition getDefinition() {
        return definition;
    }

    /** Returns the assembly language representation of this instruction. */
    public abstract String disassemble();

    // --- Field Extraction (Can be used by subclasses or Factory/Simulator) ---
    // These methods extract raw bit fields based on standard LEGv8 layouts.
    // Subclasses or the Simulator might need to interpret these (e.g., sign-extend).

    /** Extracts bits [endBit..startBit] (inclusive, LSB=0) */
    public static int extractBits(BitSet bits, int startBit, int endBit) {
        if (startBit < 0 || endBit < startBit || endBit >= 32) { // Basic range check
             // Consider throwing an exception for invalid range
             System.err.printf("Warning: Invalid bit range requested: %d to %d\n", startBit, endBit);
             return 0; // Or throw new IllegalArgumentException(...)
        }
        int value = 0;
        for (int i = startBit; i <= endBit; i++) {
            if (bits.get(i)) {
                value |= (1 << (i - startBit));
            }
        }
        return value;
    }

    // Helper to set bits for Factory/Assembler use
    protected static void setBits(BitSet bits, int value, int startBit, int endBit) {
         if (startBit < 0 || endBit < startBit || endBit >= 32) {
             System.err.printf("Warning: Invalid bit range for setting: %d to %d\n", startBit, endBit);
             return; // Or throw
         }
        int mask = 1;
        for (int i = startBit; i <= endBit; i++) {
            bits.set(i, (value & mask) != 0);
            mask <<= 1;
        }
    }

    // Common field getters based on standard formats
    // These assume the caller knows the format is appropriate.

    public int getOpcode_R() { return extractBits(bytecode, 21, 31); } // 11 bits
    public int getRm_R()     { return extractBits(bytecode, 16, 20); } // 5 bits
    public int getShamt_R()  { return extractBits(bytecode, 10, 15); } // 6 bits
    public int getRn_R()     { return extractBits(bytecode, 5, 9); }   // 5 bits
    public int getRd_R()     { return extractBits(bytecode, 0, 4); }   // 5 bits

    public int getOpcode_I() { return extractBits(bytecode, 22, 31); } // 10 bits
    public int getImmediate_I(){ return extractBits(bytecode, 10, 21); } // 12 bits
    public int getRn_I()     { return extractBits(bytecode, 5, 9); }   // 5 bits
    public int getRd_I()     { return extractBits(bytecode, 0, 4); }   // 5 bits

    public int getOpcode_D() { return extractBits(bytecode, 21, 31); } // 11 bits
    public int getAddress_D(){ return extractBits(bytecode, 12, 20); } // 9 bits (DT_address)
    public int getOp2_D()    { return extractBits(bytecode, 10, 11); } // 2 bits (Unused in LDUR/STUR)
    public int getRn_D()     { return extractBits(bytecode, 5, 9); }   // 5 bits
    public int getRt_D()     { return extractBits(bytecode, 0, 4); }   // 5 bits (destination for LDUR, source for STUR)

    public int getOpcode_B() { return extractBits(bytecode, 26, 31); } // 6 bits
    public int getAddress_B(){ return extractBits(bytecode, 0, 25); }  // 26 bits (BR_address)

    public int getOpcode_CB(){ return extractBits(bytecode, 24, 31); } // 8 bits
    public int getAddress_CB(){ return extractBits(bytecode, 5, 23); } // 19 bits (COND_BR_address)
    public int getRt_CB()    { return extractBits(bytecode, 0, 4); }  // 5 bits (Rt for CBZ/NZ, Cond for B.cond)

    public int getOpcode_IM(){ return extractBits(bytecode, 23, 31); } // 9 bits
    public int getShift_IM() { return extractBits(bytecode, 21, 22); } // 2 bits (hw: 00, 01, 10, 11 for LSL 0, 16, 32, 48)
    public int getImmediate_IM(){ return extractBits(bytecode, 5, 20); } // 16 bits
    public int getRd_IM()    { return extractBits(bytecode, 0, 4); }   // 5 bits

    // Helper for debugging
    public static String formatBitSet(BitSet bits) {
        StringBuilder sb = new StringBuilder(35); // 32 bits + 3 spaces
        for (int i = 31; i >= 0; i--) {
            sb.append(bits.get(i) ? '1' : '0');
            if (i > 0 && i % 8 == 0) {
                sb.append(' '); // Space every 8 bits
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return disassemble() + " // " + formatBitSet(bytecode);
    }
}