package legv8.simulator;

import legv8.util.ControlSignals; 
import legv8.datapath.BusID;
import legv8.datapath.ComponentID;
import java.util.Set;
import java.util.Map;

public record StepInfo (
    String description,
    ComponentID startComponent,
    ComponentID endComponent,
    BusID bus,
    String value
) {
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(description);
        sb.append("\n\tComponent: " + startComponent.toString() + " -> " + endComponent.toString());
        sb.append("\n\tBus: " + bus.toString());
        sb.append("\n\tValue: " + value + "\n");

        return sb.toString();
    }
}

