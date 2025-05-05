/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.core;

import legv8.exceptions.InvalidInstructionException;
import legv8.instructions.InstructionConfigLoader;
import legv8.instructions.InstructionDefinition;
import legv8.util.ColoredLog;
import legv8.util.ControlSignals;

import java.util.BitSet;
import java.util.Objects;

/**
 * Simulates the Control Unit of the LEGv8 CPU.
 * Its primary function is to decode an instruction's bytecode (specifically its opcode fields)
 * and generate the necessary {@link ControlSignals} to orchestrate the datapath components
 * for that instruction's execution. It relies on an {@link InstructionConfigLoader}
 * to map opcodes/formats to their corresponding definitions and control signal settings.
 */
public class ControlUnit {

    // Loader providing the mapping from opcodes/mnemonics to InstructionDefinitions and ControlSignals. 
    private final InstructionConfigLoader configLoader;


    // --- Record for Decode Result ---

    /**
     * A record to hold the results of the decoding process.
     * Contains the generated control signals and the identified instruction definition.
     * @param signals The generated ControlSignals for the instruction.
     * @param definition The InstructionDefinition matching the decoded instruction.
     */
    public record DecodeResult(
        ControlSignals signals,
        
        InstructionDefinition definition 
    ) {
        /** 
         * Provides a concise string representation of the decode result. 
         */
        @Override
        public String toString() {
            return String.format("DecodeResult{signals=%s, definition=%s}",
                                signals, definition.getMnemonic());
        }
    }


    // --- Constructor ---

    /**
     * Constructs a new ControlUnit.
     * @param configLoader The {@link InstructionConfigLoader} instance that holds the
     *                     instruction set definition and control signal mappings. Must not be null.
     * @throws NullPointerException if configLoader is null.
     */
    public ControlUnit(InstructionConfigLoader configLoader) {
        this.configLoader = Objects.requireNonNull(configLoader, ColoredLog.WARNING + "InstructionConfigLoader cannot be null.");
        System.out.println(ColoredLog.SUCCESS + "Control Unit initialized.");
    }


    // --- Public API ---

    /**
     * Decodes the given instruction bytecode to determine the appropriate control signals
     * and the instruction's definition. It attempts to match the bytecode's opcode fields
     * against known instruction formats (R, I, D, B, CB, IM) using the definitions loaded
     * by the {@code configLoader}. Special handling is included for B.cond instructions.
     *
     * @param instructionBytecode The 32-bit bytecode of the instruction to decode. Must not be null.
     * @return A {@link DecodeResult} record containing the generated {@link ControlSignals}
     *         and the identified {@link InstructionDefinition}.
     * @throws InvalidInstructionException if the bytecode does not match any known instruction
     *                                     definition in the loaded configuration.
     * @throws NullPointerException if instructionBytecode is null.
     */
    public DecodeResult decode(BitSet instructionBytecode) {
        Objects.requireNonNull(instructionBytecode, ColoredLog.WARNING + "Instruction bytecode cannot be null for decoding.");

        InstructionDefinition definition = null;
        int opcodeId;

        opcodeId = extractBits(instructionBytecode, 24, 31);
        definition = configLoader.getDefinition(opcodeId, 'C');

        if (definition == null) {
            opcodeId = extractBits(instructionBytecode, 23, 31);
            definition = configLoader.getDefinition(opcodeId, 'M');
        }

        
        if (definition == null) {
            opcodeId = extractBits(instructionBytecode, 22, 31);
            definition = configLoader.getDefinition(opcodeId, 'I');
        }

        
        if (definition == null) {
            opcodeId = extractBits(instructionBytecode, 21, 31);
            definition = configLoader.getDefinition(opcodeId, 'D');
        }

        
        if (definition == null) {
            opcodeId = extractBits(instructionBytecode, 21, 31);
            definition = configLoader.getDefinition(opcodeId, 'R');
        }

        
        if (definition == null) {
            opcodeId = extractBits(instructionBytecode, 26, 31);
            definition = configLoader.getDefinition(opcodeId, 'B');
        }

        
        if (definition == null) {
            throw new InvalidInstructionException("Control Unit could not find definition for bytecode: " + formatBitSet(instructionBytecode));
        }

        ControlSignals signals = definition.getControlSignals();
        System.out.printf("%sCU Decoded: %s -> Signals=%s\n", ColoredLog.INFO, definition.getMnemonic(), signals);

        return new DecodeResult(signals, definition);
    }


    // --- Helper Methods ---

    /**
     * Extracts a range of bits from a BitSet.
     * @param bits The BitSet to extract bits from.
     * @param startBit The starting bit index (inclusive).
     * @param endBit The ending bit index (inclusive).
     * @return The extracted bits as an integer.
     */
    private int extractBits(BitSet bits, int startBit, int endBit) {
        return legv8.instructions.Instruction.extractBits(bits, startBit, endBit);
    }

    /**
     * Formats a BitSet into a string representation.
     * @param bits The BitSet to format.
     * @return A string representation of the BitSet.
     */
    private String formatBitSet(BitSet bits) {
        return legv8.instructions.Instruction.formatBitSet(bits);
    }
}