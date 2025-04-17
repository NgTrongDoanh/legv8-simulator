package simulator.core;

import simulator.instructions.InstructionConfigLoader;
import simulator.instructions.InstructionDefinition;
import simulator.util.ALUOperation;
import simulator.util.ControlSignals;
import simulator.exceptions.InvalidInstructionException;

import java.util.BitSet;
import java.util.Objects;

/**
 * Decodes instructions to generate control signals and determine ALU operation.
 * Uses the InstructionConfigLoader to find instruction definitions.
 */
public class ControlUnit {

    private final InstructionConfigLoader configLoader;

    /** Stores the result of a decode operation. */
    public record DecodeResult(
        ControlSignals signals,
        ALUOperation aluOperation,
        InstructionDefinition definition // Include definition for context if needed later
    ) {}

    public ControlUnit(InstructionConfigLoader configLoader) {
        this.configLoader = Objects.requireNonNull(configLoader, "InstructionConfigLoader cannot be null.");
        System.out.println("Control Unit initialized.");
    }

    /**
     * Decodes the given instruction bytecode.
     *
     * @param instructionBytecode The 32-bit instruction.
     * @return A DecodeResult containing the control signals, ALU operation, and definition.
     * @throws InvalidInstructionException if the instruction cannot be decoded or definition not found.
     */
    public DecodeResult decode(BitSet instructionBytecode) {
         Objects.requireNonNull(instructionBytecode, "Instruction bytecode cannot be null for decoding.");

        // --- Try to identify format and find definition (similar to Factory logic) ---
        // Strategy: Match opcode patterns. Start with potentially more unique/longer ones.
        InstructionDefinition definition = null;
        int opcodeId;

        // 1. Try CB (8 bits: 31-24)
        opcodeId = extractBits(instructionBytecode, 24, 31);
        definition = configLoader.getDefinition(opcodeId, 'C');

        // 2. Try IM (9 bits: 31-23) if not CB
        if (definition == null) {
            opcodeId = extractBits(instructionBytecode, 23, 31);
            definition = configLoader.getDefinition(opcodeId, 'M');
        }

        // 3. Try I (10 bits: 31-22) if not CB or IM
        if (definition == null) {
            opcodeId = extractBits(instructionBytecode, 22, 31);
            definition = configLoader.getDefinition(opcodeId, 'I');
        }

        // 4. Try D (11 bits: 31-21) if not CB, IM, I
         if (definition == null) {
            opcodeId = extractBits(instructionBytecode, 21, 31);
            definition = configLoader.getDefinition(opcodeId, 'D');
        }

        // 5. Try R (11 bits: 31-21) if not CB, IM, I, D
         if (definition == null) {
             // Opcode ID is the same as D format check
            opcodeId = extractBits(instructionBytecode, 21, 31);
            definition = configLoader.getDefinition(opcodeId, 'R');
         }

        // 6. Try B (6 bits: 31-26) if none of the above
        if (definition == null) {
            opcodeId = extractBits(instructionBytecode, 26, 31);
            definition = configLoader.getDefinition(opcodeId, 'B');
        }

        // --- Check Result ---
        if (definition == null) {
            // If still no definition found after trying all patterns
            throw new InvalidInstructionException("Control Unit could not find definition for bytecode: " + formatBitSet(instructionBytecode));
        }

        // --- Extract results from definition ---
        ControlSignals signals = definition.getControlSignals();
        ALUOperation aluOperation = definition.getAluOperation();

        // Log the decoded information (optional, can be moved to Simulator)
        // System.out.printf("CU Decoded: %s -> Signals=%s, ALUOp=%s\n",
        //                   definition.getMnemonic(), signals, aluOperation);

        return new DecodeResult(signals, aluOperation, definition);
    }

    // --- Helper methods (can be static if preferred) ---

    /** Extracts bits [endBit..startBit] (inclusive, LSB=0) */
    private int extractBits(BitSet bits, int startBit, int endBit) {
         // Reusing the static helper from Instruction class for consistency
         return simulator.instructions.Instruction.extractBits(bits, startBit, endBit);
    }

    /** Formats BitSet for logging (reuse from Instruction class) */
     private String formatBitSet(BitSet bits) {
         return simulator.instructions.Instruction.formatBitSet(bits);
     }
}