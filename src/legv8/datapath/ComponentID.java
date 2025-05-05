/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.datapath;

/*
 * This enum represents the various component IDs used in the LEGv8 CPU datapath.
 * Each ID corresponds to a specific component in the datapath, facilitating
 * communication and control signals.
 * 
 * You can change the layout of the datapath by changing the file resources/layout/datapath_layout.json
 */
public enum ComponentID {
    PROGRAM_COUNTER,        // Program Counter
    INSTRUCTION_MEMORY,     // Instruction Memory
    SPLITTER,               // Instruction Splitter 
    MUX_REGFILESrc,         // MUX for Register File Source 
    REGISTERS_FILE,         // Register File
    EXTRACTOR,              // Instruction Extractor
    CONTROL_UNIT,           // Control Unit
    MUX_ALUSrc,             // MUX for ALU Source
    ALU,                    // Arithmetic Logic Unit
    ALU_CONTROL,            // ALU Control Unit
    DATA_MEMORY,            // Data Memory
    MUX_WB_REGFILE,         // MUX for Write Back to Register File
    PC_ADDER4,              // PC Adder for +4
    SHIFT_LEFT_2,           // Shift Left by 2
    BR_ADDER,               // Branch Adder
    MUX_PCSrc,              // MUX for PC Source
    N_FLAG,                 // Negative Flag
    Z_FLAG,                 // Zero Flag
    C_FLAG,                 // Carry Flag   
    V_FLAG,                 // Overflow Flag
    BR_FLAG_AND,            // Branch Flag AND
    BR_ZERO_AND,            // Branch Zero AND  
    BR_OR,                  // Branch OR
    CONSTANT_4,             // Constant 4 for PC Adder
}