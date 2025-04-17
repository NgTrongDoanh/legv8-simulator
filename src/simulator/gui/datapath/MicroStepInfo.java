package simulator.gui.datapath;

import simulator.util.ALUOperation;
import simulator.util.ControlSignals; // Giả sử ControlSignals ở util
import java.util.Set;
import java.util.Map;

/**
 * Represents a single micro-step within a CPU cycle for visualization purposes.
 */
public record MicroStepInfo(
    String description,         // Mô tả ngắn gọn (ví dụ: "PC -> IMem Addr Bus")
    Set<ComponentID> activeComponents, // Các component đang hoạt động chính trong bước này
    Set<BusID> activeBuses,      // Các đường bus đang truyền dữ liệu trong bước này
    Map<BusID, Long> busValues,    // Giá trị dữ liệu trên các bus hoạt động (nếu có)
    ControlSignals currentSignals, // Trạng thái tín hiệu điều khiển tại thời điểm này
    ALUOperation currentAluOp,   // Phép toán ALU (nếu ALU đang hoạt động)
    boolean nFlag, boolean zFlag, boolean cFlag, boolean vFlag // Trạng thái cờ (nếu ALU vừa cập nhật)
) {
     // Constructor tiện lợi hơn có thể được thêm vào
     public MicroStepInfo(String desc, ComponentID comp, BusID bus, long value, ControlSignals signals) {
         this(desc, Set.of(comp), Set.of(bus), Map.of(bus, value), signals, ALUOperation.IDLE, false, false, false, false);
     }
      public MicroStepInfo(String desc, Set<ComponentID> comps, Set<BusID> buses, ControlSignals signals) {
          this(desc, comps, buses, Map.of(), signals, ALUOperation.IDLE, false, false, false, false);
      }
     // ... thêm các constructor khác nếu cần ...
}