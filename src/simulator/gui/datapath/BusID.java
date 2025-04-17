package simulator.gui.datapath;

// Enum định danh các đường bus/kết nối quan trọng
public enum BusID {
    // Fetch
    PC_OUT1,           // Output của PC
    PC_IMEM_ADDR,     // Từ PC đến Address của IMem
    IMEM_OUT_INSTR,   // Output Instruction [31-0] từ IMem
    // Decode / Register Read
    INSTR_CTRL_BITS,  // Bits lệnh đi vào Control Unit
    INSTR_REG1_ADDR,  // Bits lệnh [9-5] đến Read Register 1
    INSTR_REG2_ADDR,  // Bits lệnh [20-16] đến Read Register 2 hoặc Mux RegDst
    INSTR_WRITE_ADDR, // Bits lệnh [4-0] (Rd) hoặc [20-16] (Rt) đến Write Register (qua Mux nếu có)
    INSTR_IMM_SEXT,   // Bits lệnh [31-21] hoặc khác đến Sign Extend
    REG_READ_DATA1,   // Output Read Data 1 từ Registers
    REG_READ_DATA2,   // Output Read Data 2 từ Registers
    SEXT_OUT,         // Output từ Sign Extend
    // Execute
    ALU_IN_A,         // Input A của ALU (Thường từ Read Data 1)
    ALU_IN_B_MUX_OUT, // Output của Mux ALUSrc đi vào Input B của ALU
    ALU_OUT_RESULT,   // Output kết quả từ ALU
    ALU_OUT_ZERO,     // Tín hiệu Zero từ ALU
    NZCV_OUT,         // Các tín hiệu cờ N,Z,C,V ra từ ALU/Flags
    // Memory
    MEM_ADDRESS,      // Địa chỉ đến Data Memory (từ ALU_OUT_RESULT)
    MEM_WRITE_DATA,   // Dữ liệu ghi vào Data Memory (từ Read Data 2)
    MEM_READ_DATA,    // Dữ liệu đọc ra từ Data Memory
    // Write Back
    WB_DATA_MUX_OUT,  // Output của Mux MemToReg/MemWB
    WB_WRITE_REG_DATA,// Dữ liệu cuối cùng ghi vào thanh ghi
    WB_WRITE_REG_ADDR,// Địa chỉ thanh ghi đích cuối cùng
    // PC Update / Branching
    PC4_OUT,          // Output của Adder PC+4
    BRANCH_OFFSET_IN, // Input vào Shifter (từ Sign Extend?)
    BRANCH_OFFSET_SHIFTED, // Output từ Shifter << 2
    BRANCH_TARGET_ADDR,// Output từ Adder tính địa chỉ nhánh
    PC_SRC_MUX_OUT,   // Output của Mux chọn PC tiếp theo
    PC_IN,            // Input vào PC (từ Mux chọn PC)
    PC_ADD4,
    PC_ADD_OFFSET,
    //Splitter
    IMEM_SP,
    SP_CTRL,
    SP_REG_1,
    SP_MUX_0,
    SP_MUX_1,
    SP_REG_2,
    // Control Signals (Có thể vẽ các đường riêng nếu muốn)
    CTRL_REGWRITE, CTRL_ALUSRC, CTRL_MEMWRITE, CTRL_MEMREAD, CTRL_MEMTOREG,
    CTRL_ZEROBRANCH, CTRL_FLAGBRANCH, CTRL_UNCONDBRANCH, CTRL_FLAGWRITE
}