package simulator.gui.datapath;

// Enum định danh các thành phần trên datapath cho việc highlight
public enum ComponentID {
    PC,
    IMEM, // Instruction Memory
    SP, // Splitter (tách instruction thành các trường)
    ID_MUX_REGDST, // Mux chọn Write Register [4-0] vs [20-16] (Nếu có theo sơ đồ)
    REG_FILE,      // Khối thanh ghi
    SIGN_EXTEND,
    ID_CTRL,       // Khối Control chính (từ instruction[31-21])
    EX_MUX_ALUSRC, // Mux chọn ALU input B (Reg vs Imm)
    EX_ALU,        // Khối ALU
    EX_ALU_CTRL,   // Khối ALU Control (từ instruction[funct] hoặc Control) - Có thể chỉ là logic, không vẽ riêng
    MEM_DATA,      // Data Memory
    WB_MUX_MEMWB,  // Mux chọn Write Back Data (ALU vs Mem)
    PC_ADD_4,      // Adder tính PC+4
    BR_SHIFT_LEFT, // Shifter tính Branch Offset << 2
    BR_ADD_TARGET, // Adder tính Branch Target Address (PC + Offset)
    BR_MUX_PCSrc,  // Mux chọn Next PC (PC+4 vs Branch Target vs Jump Target)
    N_FLAGS,    // Khối hiển thị cờ N
    Z_FLAGS,    // Khối hiển thị cờ Z
    C_FLAGS,    // Khối hiển thị cờ C,V
    V_FLAGS,    // Khối hiển thị cờ N,Z,C,V
    // Thêm các cổng logic nếu muốn highlight riêng:
    BRANCH_AND_GATE, // Cổng AND kiểm tra Zero/Control cho branch
    BRANCH_OR_GATE   // Cổng OR tổng hợp các tín hiệu branch
}