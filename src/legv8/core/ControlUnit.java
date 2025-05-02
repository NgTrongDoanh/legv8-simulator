package legv8.core;

import java.util.BitSet;
import java.util.Objects;

import legv8.exceptions.InvalidInstructionException;
import legv8.instructions.InstructionConfigLoader;
import legv8.instructions.InstructionDefinition;
import legv8.util.ColoredLog;
import legv8.util.ControlSignals;

public class ControlUnit {

    private final InstructionConfigLoader configLoader;

    public record DecodeResult(
        ControlSignals signals,
        
        InstructionDefinition definition 
    ) {
        @Override
        public String toString() {
            return String.format("DecodeResult{signals=%s, definition=%s}",
                                 signals, definition.getMnemonic());
        }
    }

    public ControlUnit(InstructionConfigLoader configLoader) {
        this.configLoader = Objects.requireNonNull(configLoader, ColoredLog.WARNING + "InstructionConfigLoader cannot be null.");
        System.out.println(ColoredLog.SUCCESS + "Control Unit initialized.");
    }

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

    

    private int extractBits(BitSet bits, int startBit, int endBit) {
        
        return legv8.instructions.Instruction.extractBits(bits, startBit, endBit);
    }

    private String formatBitSet(BitSet bits) {
        return legv8.instructions.Instruction.formatBitSet(bits);
    }
}