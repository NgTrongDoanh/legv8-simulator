package legv8.simulator;

import legv8.util.ControlSignals;
import legv8.storage.*;
import legv8.datapath.BusID;
import legv8.datapath.ComponentID;
import java.util.Set;
import java.util.Map;

public record MicroStep (       
    Set<StepInfo> stepInfo,
    MemoryStorage memoryStorage,
    RegisterStorage registerStorage,
    long programCounter
) {
    public MicroStep(Set<StepInfo> stepInfo, MemoryStorage memoryStorage, RegisterStorage registerStorage, long programCounter) {
        this.stepInfo = stepInfo;
        this.memoryStorage = memoryStorage;
        this.registerStorage = registerStorage;
        this.programCounter = programCounter;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MicroStep Details:\n");
        for (StepInfo info : stepInfo) {
            sb.append(info.toString());
        }
        sb.append("Memory Storage:\n").append(memoryStorage.toString());
        sb.append("Register Storage:\n").append(registerStorage.toString());
        sb.append("Program Counter: ").append(programCounter).append("\n");
        return sb.toString();
    }
}