/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.simulator;

import legv8.datapath.BusID;
import legv8.datapath.ComponentID;

/**
 * StepInfo is a class (record) that represents a single step in the micro-operation of the LEGv8 architecture.
 * It contains information about the current state of the memory, registers, and program counter.
 */
public record StepInfo (
    // --- Fields ---
    // A string that describes the step
    String description,
    // The starting and ending components of the step
    ComponentID startComponent,
    ComponentID endComponent,
    // The bus ID associated with the step
    BusID bus,
    // The value associated with the step
    String value
) {
    // --- Utility Methods ---
    /**
     * @return A string representation of the StepInfo object.
     *         It includes details about the step information, memory storage, register storage, and program counter.
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(description);
        sb.append("\n\tComponent: " + startComponent.toString() + " -> " + endComponent.toString());
        sb.append("\n\tBus: " + bus.toString());
        sb.append("\n\tValue: " + value + "\n");

        return sb.toString();
    }
}

