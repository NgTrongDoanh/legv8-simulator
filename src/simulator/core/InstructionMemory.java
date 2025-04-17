package simulator.core;

import simulator.exceptions.InvalidPCException;
import simulator.instructions.Instruction; // Needs Instruction class

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stores the assembled program instructions and provides fetching capability.
 */
public class InstructionMemory {

    // Share BASE_ADDRESS, maybe move to a Constants class later
    public static final long BASE_ADDRESS = ProgramCounter.BASE_ADDRESS;
    private static final int INSTRUCTION_BYTES = 4;

    private List<Instruction> instructions;
    // Optional: Keep original assembly lines mapped to addresses for display?

    public InstructionMemory() {
        this.instructions = new ArrayList<>();
        System.out.println("Instruction Memory initialized.");
    }

    /**
     * Loads a list of pre-assembled Instruction objects into memory.
     * Clears any existing instructions.
     *
     * @param assembledInstructions List of Instruction objects. Cannot be null.
     */
    public void loadInstructions(List<Instruction> assembledInstructions) {
        Objects.requireNonNull(assembledInstructions, "Assembled instruction list cannot be null.");
        this.instructions.clear();
        this.instructions.addAll(assembledInstructions);
        System.out.printf("Instruction Memory loaded with %d instructions.\n", this.instructions.size());
        // displayMemorySummary(); // Optional: Log loaded instructions
    }

    /**
     * Fetches the Instruction at the specified byte address.
     *
     * @param byteAddress The address (must be >= BASE_ADDRESS and word-aligned).
     * @return The Instruction object.
     * @throws InvalidPCException if the address is invalid (out of bounds, misaligned, negative).
     */
    public Instruction fetch(long byteAddress) {
        if (byteAddress < BASE_ADDRESS) {
            throw new InvalidPCException("Instruction fetch address 0x" + Long.toHexString(byteAddress) +
                                         " is below base address 0x" + Long.toHexString(BASE_ADDRESS), byteAddress);
        }
        if ((byteAddress - BASE_ADDRESS) % INSTRUCTION_BYTES != 0) {
             throw new InvalidPCException("Instruction fetch address 0x" + Long.toHexString(byteAddress) +
                                         " is not word-aligned (multiple of " + INSTRUCTION_BYTES + ") relative to base.", byteAddress);
        }

        long index = (byteAddress - BASE_ADDRESS) / INSTRUCTION_BYTES;

        if (index < 0 || index >= instructions.size()) {
             throw new InvalidPCException(
                 String.format("Instruction fetch address 0x%X (index %d) is out of bounds [0..%d]",
                               byteAddress, index, instructions.isEmpty() ? -1 : instructions.size() - 1),
                 byteAddress
             );
        }

        Instruction instruction = instructions.get((int)index);
        if (instruction == null) {
            // This shouldn't happen if loadInstructions checks for nulls, but safeguard
            throw new InvalidPCException("Fetched null instruction at address 0x" + Long.toHexString(byteAddress), byteAddress);
        }

        // Optional Logging (move to Simulator for better control)
        // System.out.printf("IMEM Fetch @ 0x%X -> %s\n", byteAddress, instruction.disassemble());
        return instruction;
    }

    /** Returns the number of instructions loaded. */
    public int getInstructionCount() {
        return instructions.size();
    }

     /** Returns an unmodifiable view of the loaded instructions. */
    public List<Instruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    /** Clears all loaded instructions. */
    public void clear() {
        instructions.clear();
    }

    /** Displays a summary of loaded instructions (for debugging). */
    public void displayMemorySummary() {
        System.out.println("--- Instruction Memory Contents ---");
        if (instructions.isEmpty()) {
            System.out.println("  (Empty)");
            return;
        }
        for (int i = 0; i < instructions.size(); i++) {
            long addr = BASE_ADDRESS + (long)i * INSTRUCTION_BYTES;
            System.out.printf("  0x%08X : %s\n", addr, instructions.get(i)); // Uses Instruction.toString()
        }
        System.out.println("---------------------------------");
    }
}