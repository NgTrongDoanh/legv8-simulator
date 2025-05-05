/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.simulator;

import legv8.storage.*;

import java.util.Set;

/**
 * MicroStep is a class (record) that represents a single step in the micro-operation of the LEGv8 architecture.
 * It contains information about the current state of the memory, registers, and program counter.
 */
public record MicroStep (      
    // --- Fields ---
    // A set of StepInfo objects that provide information about the current step 
    Set<StepInfo> stepInfo,
    // MemoryStorage object that represents the current state of memory
    MemoryStorage memoryStorage,
    // RegisterStorage object that represents the current state of registers
    RegisterStorage registerStorage,
    // The program counter indicating the current instruction
    long programCounter
) {
    // --- Constructor ---
    /**
     * Constructor for MicroStep.
     * @param stepInfo A set of StepInfo objects that provide information about the current step.
     * @param memoryStorage The current state of memory.
     * @param registerStorage The current state of registers.
     * @param programCounter The program counter indicating the current instruction.
     */
    public MicroStep(Set<StepInfo> stepInfo, MemoryStorage memoryStorage, RegisterStorage registerStorage, long programCounter) {
        this.stepInfo = stepInfo;
        this.memoryStorage = memoryStorage;
        this.registerStorage = registerStorage;
        this.programCounter = programCounter;
    }

    // --- Utility Methods ---
    /**
     * @return A string representation of the MicroStep object.
     *         It includes details about the step information, memory storage, register storage, and program counter.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("MicroStep Details:\n");
        for (StepInfo info : stepInfo) sb.append(info.toString()); 

        sb.append("Memory Storage:\n").append(memoryStorage.toString());
        sb.append("Register Storage:\n").append(registerStorage.toString());
        sb.append("Program Counter: ").append(programCounter).append("\n");
        
        return sb.toString();
    }
}