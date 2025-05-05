/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.core;

import legv8.exceptions.InvalidPCException;
import legv8.instructions.Instruction;
import legv8.util.ColoredLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Simulates the Instruction Memory of the LEGv8 CPU.
 * It stores a list of assembled instructions and provides methods to load,
 * fetch, and display them. The instructions are stored in a list and can be
 * accessed using their byte address, which is validated for alignment and
 * bounds.
 */
public class InstructionMemory {
    /** The base address for instruction memory. */
    private static final int INSTRUCTION_BYTES = 4;

    /** The list of instructions stored in memory. */
    private List<Instruction> instructions;


    // --- Constructor ---

    /**
     * Constructs a new InstructionMemory instance.
     * Initializes an empty list of instructions.
     */
    public InstructionMemory() {
        this.instructions = new ArrayList<>();
        System.out.println(ColoredLog.SUCCESS + "Instruction Memory initialized.");
    }


    // --- Public API ---
    
    /**
     * Loads a list of assembled instructions into the instruction memory.
     * Clears any existing instructions before loading the new ones.
     *
     * @param assembledInstructions The list of assembled instructions to load.
     * @throws NullPointerException if the provided list is null.
     */
    public void loadInstructions(List<Instruction> assembledInstructions) {
        Objects.requireNonNull(assembledInstructions, ColoredLog.WARNING + "Assembled instruction list cannot be null.");
        this.instructions.clear();
        this.instructions.addAll(assembledInstructions);
        System.out.printf("%sInstruction Memory loaded with %d instructions.\n", ColoredLog.SUCCESS, this.instructions.size());
        
    }

    /**
     * Fetches an instruction from memory at the specified byte address.
     * Validates the address for alignment and bounds before fetching.
     *
     * @param byteAddress The byte address of the instruction to fetch.
     * @return The fetched instruction.
     * @throws InvalidPCException if the address is invalid (out of bounds or misaligned).
     */
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

    /**
     * Returns the number of instructions currently loaded in memory.
     * @return The count of instructions in memory.
     */
    public int getInstructionCount() {
        return instructions.size();
    }

    /**
     * Returns an unmodifiable list of instructions currently loaded in memory.
     * @return An unmodifiable list of instructions.
     */
    // Note: This method returns an unmodifiable list to prevent external modification.    
    //       If you need to modify the list, use the loadInstructions method instead.
    public List<Instruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    /**
     * Clears the instruction memory, removing all loaded instructions.
     * This method is typically used for resetting or reloading the memory.
     */
    public void clear() {
        instructions.clear();
    }

    /**
     * Displays a summary of the instruction memory contents.
     * Prints the address and disassembled instruction for each entry in memory.
     * If the memory is empty, it indicates that as well.
     */    
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