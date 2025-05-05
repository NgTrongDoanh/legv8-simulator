/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.datapath;

/**
 * Enum representing the various bus IDs used in the LEGv8 CPU datapath.
 * Each ID corresponds to a specific connection or data flow between components
 * in the datapath, facilitating communication and control signals.
 * 
 * You can change the layout of the datapath by changing the file resources/layout/datapath_layout.json
 */
public enum BusID {
    ProgramCounter_InstructionMemory,               // PC to Instruction Memory
    ProgramCounter_PCAdder4,                        // PC to PC Adder (+4)
    ProgramCounter_BranchAdder,                     // PC to Branch Adder
    InstructionMemory_Splitter,                     // Instruction Memory to Splitter
    Splitter_ControlUnit,                           // Splitter to Control Unit
    Splitter_MuxRegFile_0,                          // Splitter to MuxRegFile (0 signal)
    Splitter_MuxRegFile_1,                          // Splitter to MuxRegFile (1 signal)
    Splitter_RegFile1,                              // Splitter to RegFile
    Splitter_RegFile2,                              // Splitter to RegFile
    Splitter_Extractor,                             // Splitter to Extractor
    Splitter_AluControl,                            // Splitter to AluControl
    MuxRegFile_RegFile,                             // MuxRegFile to RegFile
    RegFile_Alu,                                    // RegFile to ALU
    RegFile_MuxAlu_0,                               // RegFile to MuxAlu (0 signal)
    RegFile_DataMemory,                             // RegFile to DataMemory
    Extractor_ShiftLeft2,                           // Extractor to ShiftLeft2
    Extractor_MuxAlu_1,                             // Extractor to MuxAlu (1 signal)
    MuxAlu_Alu,                                     // MuxAlu to ALU
    Alu_NFlag,                                      // ALU to NFlag
    Alu_ZFlag,                                      // ALU to ZFlag
    Alu_CFlag,                                      // ALU to CFlag
    Alu_VFlag,                                      // ALU to VFlag
    Alu_BrZeroAnd,                                  // ALU to BrZeroAnd
    Alu_DataMemory,                                 // ALU to DataMemory
    Alu_MuxWbRegFile_0,                             // ALU to MuxWbRegFile (0 signal)
    AluControl_Alu_Signal,                          // AluControl to ALU
    NFlag_BrFlagAnd,                                // NFlag to BrFlagAnd
    ZFlag_BrFlagAnd,                                // ZFlag to BrFlagAnd
    CFlag_BrFlagAnd,                                // CFlag to BrFlagAnd
    VFlag_BrFlagAnd,                                // VFlag to BrFlagAnd
    DataMemory_MuxWbRegFile_1,                      // DataMemory to MuxWbRegFile (1 signal)
    MuxWbRegFile_RegFile,                           // MuxWbRegFile to RegFile
    ShiftLeft2_BranchAdder,                         // ShiftLeft2 to BranchAdder
    BranchAdder_MuxPCSrc_1,                         // BranchAdder to MuxPCSrc (1 signal)
    PCAdder4_MuxPCSrc_0,                            // PCAdder4 to MuxPCSrc (0 signal)
    PCAdder4_const,                                 // Constant (4) for PCAdder4
    MuxPCSrc_ProgramCounter,                        // MuxPCSrc to ProgramCounter
    BrFlagAnd_BrOr,                                 // BrFlagAnd to BrOr
    BrZeroAnd_BrOr,                                 // BrZeroAnd to BrOr
    BrOr_MuxPCSrc_Signal,                           // BrOr to MuxPCSrc (Signal)
    ControlUnit_MuxRegFile_Signal_Reg2Loc,          // ControlUnit to MuxRegFile (Reg2Loc)
    ControlUnit_BrOr_Signal_UncondBranch,           // ControlUnit to BrOr (UncondBranch)
    ControlUnit_BrFlagAnd_Signal_FlagBranch,        // ControlUnit to BrFlagAnd (FlagBranch)
    ControlUnit_BrZeroAnd_Signal_ZeroBranch,        // ControlUnit to BrZeroAnd (ZeroBranch)
    ControlUnit_DataMemory_Signal_MemRead,          // ControlUnit to DataMemory (MemRead)
    ControlUnit_MuxWbRegFile_Signal_MemToReg,       // ControlUnit to MuxWbRegFile (MemToReg)
    ControlUnit_DataMemory_Signal_MemWrite,         // ControlUnit to DataMemory (MemWrite)
    ControlUnit_Flags_Signal_FlagWrite,             // ControlUnit to Flags (FlagWrite)
    ControlUnit_MuxAlu_Signal_AluSrc,               // ControlUnit to MuxAlu (AluSrc)
    ControlUnit_AluControl_Signal_AluOp,            // ControlUnit to AluControl (AluOp)
    ControlUnit_RegFile_Signal_RegWrite             // ControlUnit to RegFile (RegWrite)
}
