package legv8.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import legv8.exceptions.InvalidPCException;
import legv8.instructions.Instruction;
import legv8.util.ColoredLog;

public class InstructionMemory {
    private static final int INSTRUCTION_BYTES = 4;

    private List<Instruction> instructions;

    public InstructionMemory() {
        this.instructions = new ArrayList<>();
        System.out.println(ColoredLog.SUCCESS + "Instruction Memory initialized.");
    }

    public void loadInstructions(List<Instruction> assembledInstructions) {
        Objects.requireNonNull(assembledInstructions, ColoredLog.WARNING + "Assembled instruction list cannot be null.");
        this.instructions.clear();
        this.instructions.addAll(assembledInstructions);
        System.out.printf("%sInstruction Memory loaded with %d instructions.\n", ColoredLog.SUCCESS, this.instructions.size());
        
    }

    public Instruction fetch(long byteAddress) {
        if (byteAddress < ProgramCounter.BASE_ADDRESS) {
            throw new InvalidPCException("Instruction fetch address 0x" + Long.toHexString(byteAddress) +
                                         " is below base address 0x" + Long.toHexString(ProgramCounter.BASE_ADDRESS), byteAddress);
        }
        if ((byteAddress - ProgramCounter.BASE_ADDRESS) % INSTRUCTION_BYTES != 0) {
            throw new InvalidPCException("Instruction fetch address 0x" + Long.toHexString(byteAddress) +
                                         " is not word-aligned (multiple of " + INSTRUCTION_BYTES + ") relative to base.", byteAddress);
        }

        long index = (byteAddress - ProgramCounter.BASE_ADDRESS) / INSTRUCTION_BYTES;

        if (index < 0 || index >= instructions.size()) {
            throw new InvalidPCException(String.format("Instruction fetch address 0x%X (index %d) is out of bounds [0..%d]",
                                                        byteAddress, index, instructions.isEmpty() ? -1 : instructions.size() - 1), byteAddress);
        }

        Instruction instruction = instructions.get((int)index);
        if (instruction == null) {
            throw new InvalidPCException("Fetched null instruction at address 0x" + Long.toHexString(byteAddress), byteAddress);
        }

        
        System.out.printf("%s(InstructionMemory): Fetch @ 0x%X -> %s\n", ColoredLog.INFO, byteAddress, instruction.disassemble());
        return instruction;
    }


    
    public int getInstructionCount() {
        return instructions.size();
    }


    
    public List<Instruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }


    
    public void clear() {
        instructions.clear();
    }


    
    public void displayMemorySummary() {
        System.out.println(ColoredLog.INFO + "Instruction Memory Summary:");
        System.out.println("--- Instruction Memory Contents ---");
        if (instructions.isEmpty()) {
            System.out.println("  (Empty)");
            return;
        }
        for (int i = 0; i < instructions.size(); i++) {
            long addr = ProgramCounter.BASE_ADDRESS + (long)i * INSTRUCTION_BYTES;
            System.out.printf("  0x%08X : %s\n", addr, instructions.get(i)); 
        }
        System.out.println("---------------------------------");
    }
}