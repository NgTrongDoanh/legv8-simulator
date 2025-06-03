/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.simulator;

import legv8.core.*;
import legv8.datapath.BusID;
import legv8.datapath.ComponentID;
import legv8.exceptions.*;
import legv8.instructions.*;
import legv8.storage.*;
import legv8.util.*;

import java.util.*;

/**
 * The core LEGv8 CPU simulation engine.
 * Manages the processor's state (PC, registers, memory, flags) and
 * executes instructions by simulating datapath micro-steps based on
 * control signals derived from instruction definitions. It interacts with
 * various CPU components like InstructionMemory, RegisterFileController, ALU, etc.
 */
public class SimulatorEngine {
    // --- Core Components ---
    InstructionConfigLoader configLoader;
    private final ProgramCounter programCounter;
    private final InstructionMemory instructionMemory;
    private final RegisterFileController registerController;
    // private final ArithmeticLogicUnit alu;
    private final DataMemoryController memoryController;

    // --- Simulation State ---
    // Instruction currentInstruction = null; // The instruction currently being executed
    // ControlSignals controlSignals = null; // Control signals derived from the current instruction
    // private long cycleCount = 0;
    // private boolean halted = false;
    // private boolean externalHaltRequest = false;
    
    // --- Processor Flags ---
    private boolean flagN = false; 
    private boolean flagZ = false; 
    private boolean flagC = false; 
    private boolean flagV = false; 

    // --- Micro-Step Execution ---
    private final List<MicroStep> microSteps;


    
    // --- Constructors ---

    /**
     * Constructs a new SimulatorEngine with default internal components
     * (ProgramCounter, InstructionMemory, ControlUnit, RegisterFileController,
     * ALU, DataMemoryController).
     * @param configLoader The pre-loaded instruction configuration loader. Must not be null.
     */
    public SimulatorEngine(InstructionConfigLoader configLoader) {
        this.configLoader = Objects.requireNonNull(configLoader, "InstructionConfigLoader cannot be null.");
        this.programCounter = new ProgramCounter();
        this.instructionMemory = new InstructionMemory();
        this.registerController = new RegisterFileController(new RegisterStorage());
        // this.alu = new ArithmeticLogicUnit();
        this.memoryController = new DataMemoryController(new MemoryStorage());
        microSteps = new ArrayList<>();
        
        System.out.println(ColoredLog.SUCCESS + "LEGv8 CPU Simulator Engine initialized.");
        resetState();
    }

    
    /**
     * Constructs a new SimulatorEngine allowing injection of pre-configured
     * memory and register components.
     * @param configLoader The pre-loaded instruction configuration loader. Must not be null.
     * @param instructionMemory The instruction memory unit. Must not be null.
     * @param registerController The register file controller. Must not be null.
     * @param memoryController The data memory controller. Must not be null.
     */
    public SimulatorEngine(InstructionConfigLoader configLoader, InstructionMemory instructionMemory, 
                            RegisterFileController registerController, DataMemoryController memoryController) {
        this.configLoader = Objects.requireNonNull(configLoader, "InstructionConfigLoader cannot be null.");
        this.programCounter = new ProgramCounter();
        this.instructionMemory = Objects.requireNonNull(instructionMemory, "InstructionMemory cannot be null.");
        this.registerController = Objects.requireNonNull(registerController, "RegisterFileController cannot be null.");
        // this.alu = new ArithmeticLogicUnit();
        this.memoryController = Objects.requireNonNull(memoryController, "DataMemoryController cannot be null.");
        microSteps = new ArrayList<>();

        System.out.println(ColoredLog.SUCCESS + "LEGv8 CPU Simulator Engine initialized.");
        resetState();
    }   

    /**
     * Constructs a new SimulatorEngine allowing injection of pre-loaded instruction memory.
     * Creates new register and data memory components internally.
     * @param configLoader The pre-loaded instruction configuration loader. Must not be null.
     * @param instructionMemory The instruction memory unit. Must not be null.
     */
    public SimulatorEngine(InstructionConfigLoader configLoader, InstructionMemory instructionMemory) {
        this.configLoader = Objects.requireNonNull(configLoader, "InstructionConfigLoader cannot be null.");
        this.programCounter = new ProgramCounter();
        this.instructionMemory = Objects.requireNonNull(instructionMemory, "InstructionMemory cannot be null.");
        this.registerController = new RegisterFileController(new RegisterStorage());
        // this.alu = new ArithmeticLogicUnit();
        this.memoryController = new DataMemoryController(new MemoryStorage());
        microSteps = new ArrayList<>();
        
        System.out.println(ColoredLog.SUCCESS + "LEGv8 CPU Simulator Engine initialized.");
        resetState();
    }

    
    // --- State Management ---

    /**
     * Resets the internal simulator state variables (cycle count, flags, halted status).
     * Called by the public reset() method.
     */
    private void resetState() {
        // cycleCount = 0;
        // halted = false;
        // externalHaltRequest = false;
        flagN = false;
        flagZ = false;
        flagC = false;
        flagV = false;
        // currentInstruction = null;
        // controlSignals = null;
        microSteps.clear();
        System.out.println(ColoredLog.INFO + "Simulator internal state flags reset.");
    }

    /**
     * Resets the entire simulator state to its initial values, including
     * PC (to base address), registers (cleared), data memory (cleared),
     * internal flags, and cycle count.
     */
    public void reset() {
        System.out.println(ColoredLog.START_PROCESS + "--- Resetting Simulator ---");
        programCounter.reset();
        registerController.clearStorage();
        memoryController.clearStorage();
        resetState();
        System.out.println(ColoredLog.END_PROCESS + "Simulator Reset Complete.");
    }

    private long PC_Output() {
        long currentPC = programCounter.getCurrentAddress(); // PC Out

        StepInfo pc_adder4 = new StepInfo(
            "[ProgramCounter_PCAdder4]: PROGRAM_COUNTER -> PC_ADDER4", 
            ComponentID.PROGRAM_COUNTER, ComponentID.PC_ADDER4, BusID.ProgramCounter_PCAdder4,
            "0x" + Long.toHexString(currentPC)
        );
        StepInfo pc_branch = new StepInfo(
            "[ProgramCounter_BranchAdder]: PROGRAM_COUNTER -> BR_ADDER",
            ComponentID.PROGRAM_COUNTER, ComponentID.BR_ADDER, BusID.ProgramCounter_BranchAdder,
            "0x" + Long.toHexString(currentPC)
        );
        StepInfo pc_imem = new StepInfo(
            "[ProgramCounter_InstructionMemory]: PROGRAM_COUNTER -> INSTRUCTION_MEMORY",
            ComponentID.PROGRAM_COUNTER, ComponentID.INSTRUCTION_MEMORY, BusID.ProgramCounter_InstructionMemory,
            "0x" + Long.toHexString(currentPC)
        );

        microSteps.add(new MicroStep(
            Set.of(pc_imem, pc_adder4, pc_branch),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return currentPC;
    }

    private Instruction IMEM_execute(long currentPC) {
        Instruction currentInstruction = instructionMemory.fetch(currentPC);

        StepInfo imem_sp = new StepInfo(
            "[InstructionMemory_Splitter]: INSTRUCTION_MEMORY -> SPLITTER", 
            ComponentID.INSTRUCTION_MEMORY, ComponentID.SPLITTER, BusID.InstructionMemory_Splitter, 
            currentInstruction.toString()
        );

        microSteps.add(new MicroStep(Set.of(imem_sp),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return currentInstruction;
    }

    private BitSet splitInstruction(Instruction instruction) {
        Objects.requireNonNull(instruction, "Instruction cannot be null.");
        BitSet instructionBits = instruction.getBytecode();

        int toControlUnit = Instruction.extractBits(instructionBits, 21, 31);
        StepInfo splitter_ctrl = new StepInfo("[Splitter_ControlUnit]: SPLITTER -> CONTROL_UNIT",
            ComponentID.SPLITTER, ComponentID.CONTROL_UNIT, BusID.Splitter_ControlUnit,
            String.format("%11s", Integer.toBinaryString(toControlUnit)).replace(' ', '0')
        );

        int regfile_readReg1 = Instruction.extractBits(instructionBits, 5, 9);
        StepInfo sp_regfile1 = new StepInfo(
            "[Splitter_ReadReg1]: SPLITTER -> REGISTERS_FILE",
            ComponentID.SPLITTER, ComponentID.REGISTERS_FILE, BusID.Splitter_RegFile1, 
            String.format("%5s", Integer.toBinaryString(regfile_readReg1)).replace(' ', '0')
        );

        int muxReg_0 = Instruction.extractBits(instructionBits, 16, 20);
        StepInfo sp_muxregfile_0 = new StepInfo(
            "[Splitter_MuxRegFile_0]: SPLITTER -> MUX_REGFILESrc",
            ComponentID.SPLITTER, ComponentID.MUX_REGFILESrc, BusID.Splitter_MuxRegFile_0, 
            String.format("%5s", Integer.toBinaryString(muxReg_0)).replace(' ', '0')
        );

        int muxReg_1 = Instruction.extractBits(instructionBits, 0, 4);
        StepInfo sp_muxregfile_1 = new StepInfo(
            "[Splitter_MuxRegFile_1]: SPLITTER -> MUX_REGFILESrc",
            ComponentID.SPLITTER, ComponentID.MUX_REGFILESrc, BusID.Splitter_MuxRegFile_1, 
            String.format("%5s", Integer.toBinaryString(muxReg_1)).replace(' ', '0')
        );
        
        int regfile_writeReg = Instruction.extractBits(instructionBits, 0, 4);
        StepInfo sp_regfile2 = new StepInfo(
            "[Splitter_WriteReg]: SPLITTER -> REGISTERS_FILE",
            ComponentID.SPLITTER, ComponentID.REGISTERS_FILE, BusID.Splitter_RegFile2, 
            String.format("%5s", Integer.toBinaryString(regfile_writeReg)).replace(' ', '0')
        );

        int extractor_in = Instruction.extractBits(instructionBits, 0, 31);
        StepInfo sp_signextend = new StepInfo(
            "[Splitter_Extractor]: SPLITTER -> EXTRACTOR",
            ComponentID.SPLITTER, ComponentID.EXTRACTOR, BusID.Splitter_Extractor, 
            String.format("%32s", Integer.toBinaryString(extractor_in)).replace(' ', '0')
        );

        int aluCtrl_in = Instruction.extractBits(instructionBits, 21, 31);
        StepInfo sp_aluCtrl = new StepInfo(
            "[Splitter_AluControl]: SPLITTER -> ALU_CONTROL",
            ComponentID.SPLITTER, ComponentID.ALU_CONTROL, BusID.Splitter_AluControl, 
            String.format("%11s", Integer.toBinaryString(aluCtrl_in)).replace(' ', '0')
        );

        microSteps.add(new MicroStep(
            Set.of(splitter_ctrl, sp_muxregfile_0, sp_muxregfile_1, sp_regfile1, sp_regfile2, sp_signextend, sp_aluCtrl),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return instructionBits; // Return the BitSet representation of the instruction
    }

    private ControlSignals controlUnit_execute(Instruction instruction) {
        Objects.requireNonNull(instruction, "Instruction cannot be null.");
        InstructionDefinition definition = instruction.getDefinition();
        ControlSignals controlSignals = definition.getControlSignals();
        Set<StepInfo> controlUnitSteps = new HashSet<>();

        if (controlSignals.reg2Loc() != 'x') {
            StepInfo cu_reg2Loc = new StepInfo(
                "[ControlUnit_MuxRegFile_Signal_Reg2Loc]: CONTROL_UNIT -> MUX_REGFILESrc",
                ComponentID.CONTROL_UNIT, ComponentID.MUX_REGFILESrc, BusID.ControlUnit_MuxRegFile_Signal_Reg2Loc, 
                String.valueOf(controlSignals.reg2Loc())
            );
            controlUnitSteps.add(cu_reg2Loc);
        }

        if (controlSignals.uncondBranch() != 'x') {
            StepInfo cu_uncondBranch = new StepInfo(
                "[ControlUnit_BrOr_Signal_UncondBranch]: CONTROL_UNIT -> BR_OR",
                ComponentID.CONTROL_UNIT, ComponentID.BR_OR, BusID.ControlUnit_BrOr_Signal_UncondBranch, 
                String.valueOf(controlSignals.uncondBranch())
            );
            controlUnitSteps.add(cu_uncondBranch);
        }

        if (controlSignals.flagBranch() != 'x') {
            StepInfo ctrl_brFlag = new StepInfo(
                "[ControlUnit_BrFlagAnd_Signal_FlagBranch]: CONTROL_UNIT -> BR_FLAG_AND",
                ComponentID.CONTROL_UNIT, ComponentID.BR_FLAG_AND, BusID.ControlUnit_BrFlagAnd_Signal_FlagBranch,
                String.valueOf(controlSignals.flagBranch())
            );
            controlUnitSteps.add(ctrl_brFlag);
        }

        if (controlSignals.zeroBranch() != 'x') {
            StepInfo ctrl_brZero = new StepInfo(
                "[ControlUnit_BrZeroAnd_Signal_ZeroBranch]: CONTROL_UNIT -> BR_ZERO_AND",
                ComponentID.CONTROL_UNIT, ComponentID.BR_ZERO_AND, BusID.ControlUnit_BrZeroAnd_Signal_ZeroBranch,
                String.valueOf(controlSignals.zeroBranch())
            );
            controlUnitSteps.add(ctrl_brZero);
        }

        if (controlSignals.memRead() != 'x') {
            StepInfo ctrl_memRead = new StepInfo(
                "[ControlUnit_DataMemory_Signal_MemRead]: CONTROL_UNIT -> DATA_MEMORY",
                ComponentID.CONTROL_UNIT, ComponentID.DATA_MEMORY, BusID.ControlUnit_DataMemory_Signal_MemRead,
                String.valueOf(controlSignals.memRead())
            );
            controlUnitSteps.add(ctrl_memRead);
        }

        if (controlSignals.memToReg() != 'x') {
            StepInfo ctrl_memToReg = new StepInfo(
                "[ControlUnit_MuxWbRegFile_Signal_MemToReg]: CONTROL_UNIT -> MUX_WB_REGFILE",
                ComponentID.CONTROL_UNIT, ComponentID.MUX_WB_REGFILE, BusID.ControlUnit_MuxWbRegFile_Signal_MemToReg,
                String.valueOf(controlSignals.memToReg())
            );
            controlUnitSteps.add(ctrl_memToReg);
        }

        if (controlSignals.memWrite() != 'x') {
            StepInfo ctrl_memWrite = new StepInfo(
                "[ControlUnit_DataMemory_Signal_MemWrite]: CONTROL_UNIT -> DATA_MEMORY",
                ComponentID.CONTROL_UNIT, ComponentID.DATA_MEMORY, BusID.ControlUnit_DataMemory_Signal_MemWrite,
                String.valueOf(controlSignals.memWrite())
            );
            controlUnitSteps.add(ctrl_memWrite);
        }

        if (controlSignals.flagWrite() != 'x') {
            StepInfo ctrl_flagWrite_n = new StepInfo(
                "[ControlUnit_Flags_Signal_FlagWrite]: CONTROL_UNIT -> FLAGS",
                ComponentID.CONTROL_UNIT, ComponentID.N_FLAG, BusID.ControlUnit_Flags_Signal_FlagWrite,
                String.valueOf(controlSignals.flagWrite())
            );
            controlUnitSteps.add(ctrl_flagWrite_n);
            StepInfo ctrl_flagWrite_z = new StepInfo(
                "[ControlUnit_Flags_Signal_FlagWrite]: CONTROL_UNIT -> FLAGS",
                ComponentID.CONTROL_UNIT, ComponentID.Z_FLAG, BusID.ControlUnit_Flags_Signal_FlagWrite,
                String.valueOf(controlSignals.flagWrite())
            );
            controlUnitSteps.add(ctrl_flagWrite_z);
            StepInfo ctrl_flagWrite_c = new StepInfo(
                "[ControlUnit_Flags_Signal_FlagWrite]: CONTROL_UNIT -> FLAGS",
                ComponentID.CONTROL_UNIT, ComponentID.C_FLAG, BusID.ControlUnit_Flags_Signal_FlagWrite,
                String.valueOf(controlSignals.flagWrite())
            );
            controlUnitSteps.add(ctrl_flagWrite_c);
            StepInfo ctrl_flagWrite_v = new StepInfo(
                "[ControlUnit_Flags_Signal_FlagWrite]: CONTROL_UNIT -> FLAGS",
                ComponentID.CONTROL_UNIT, ComponentID.V_FLAG, BusID.ControlUnit_Flags_Signal_FlagWrite,
                String.valueOf(controlSignals.flagWrite())
            );
            controlUnitSteps.add(ctrl_flagWrite_v);
        }

        if (controlSignals.aluSrc() != 'x') {
            StepInfo ctrl_aluSrc = new StepInfo(
                "[ControlUnit_MuxAlu_Signal_AluSrc]: CONTROL_UNIT -> MUX_ALUSrc",
                ComponentID.CONTROL_UNIT, ComponentID.MUX_ALUSrc, BusID.ControlUnit_MuxAlu_Signal_AluSrc,
                String.valueOf(controlSignals.aluSrc())
            );
            controlUnitSteps.add(ctrl_aluSrc);
        }

        if (controlSignals.aluOp() != 404) {
            StepInfo ctrl_aluOp = new StepInfo(
                "[ControlUnit_AluControl_Signal_AluOp]: CONTROL_UNIT -> ALU_CONTROL",
                ComponentID.CONTROL_UNIT, ComponentID.ALU_CONTROL, BusID.ControlUnit_AluControl_Signal_AluOp,
                String.format("%2s", Integer.toBinaryString(controlSignals.aluOp())).replace(' ', '0')
            );
            controlUnitSteps.add(ctrl_aluOp);
        }

        if (controlSignals.regWrite() != 'x') {
            StepInfo ctrl_regWrite = new StepInfo(
                "[ControlUnit_RegFile_Signal_RegWrite]: CONTROL_UNIT -> REGISTERS_FILE",
                ComponentID.CONTROL_UNIT, ComponentID.REGISTERS_FILE, BusID.ControlUnit_RegFile_Signal_RegWrite,
                String.valueOf(controlSignals.regWrite())
            );
            controlUnitSteps.add(ctrl_regWrite);
        }

        microSteps.add(new MicroStep(controlUnitSteps,
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return controlSignals; 
    }

    private int muxRecFile_execute(int muxReg_0, int muxReg_1, char reg2Loc) {
        if (reg2Loc == 'x') return -1;

        int selectedReg = (reg2Loc == '1') ? muxReg_1 : muxReg_0;
        StepInfo muxRegFile = new StepInfo(
            "[MuxRegFile]: MUX_REGFILESrc -> REGISTERS_FILE",
            ComponentID.MUX_REGFILESrc, ComponentID.REGISTERS_FILE, BusID.MuxRegFile_RegFile,
            String.format("%5s", Integer.toBinaryString(selectedReg)).replace(' ', '0')
        );

        microSteps.add(new MicroStep(
            Set.of(muxRegFile),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));
        
        return selectedReg; 
    }

    private long[] read_Regfile(int readReg_1, int readReg_2) {
        long[] values = new long[2];
        Set<StepInfo> readSteps = new HashSet<>();

        values[0] = registerController.readRegister(readReg_1);
        StepInfo regfile_alu = new StepInfo(
            "[RegFile_Alu]: REGISTERS_FILE -> ALU",
            ComponentID.REGISTERS_FILE, ComponentID.ALU, BusID.RegFile_Alu,
            String.format("0x%X (%d)", values[0], values[0])
        );
        readSteps.add(regfile_alu);

        if (readReg_2 != -1) {
            values[1] = registerController.readRegister(readReg_2);
        
            StepInfo regfile_muxAlu = new StepInfo(
                "[RegFile_MuxAlu_0]: REGISTERS_FILE -> MUX_ALUSrc",
                ComponentID.REGISTERS_FILE, ComponentID.MUX_ALUSrc, BusID.RegFile_MuxAlu_0,
                String.format("0x%X (%d)", values[1], values[1])
            );
            readSteps.add(regfile_muxAlu);

            StepInfo regfile_dataMem = new StepInfo(
                "[RegFile_DataMemory]: REGISTERS_FILE -> DATA_MEMORY",
                ComponentID.REGISTERS_FILE, ComponentID.DATA_MEMORY, BusID.RegFile_DataMemory,
                String.format("0x%X (%d)", values[1], values[1])
            );
            readSteps.add(regfile_dataMem);
        }

        microSteps.add(new MicroStep(
            readSteps,
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return values;
    }

    private long extractor_execute(BitSet instructionBits, char format, String mnemonic) {
        Objects.requireNonNull(instructionBits, "Instruction BitSet cannot be null.");

        long extendedValue = Extractor.extractAndExtend(instructionBits, format, mnemonic);

        StepInfo extractor_alu = new StepInfo(
            "[Extractor]: EXTRACTOR -> ALU",
            ComponentID.EXTRACTOR, ComponentID.ALU, BusID.Extractor_MuxAlu_1,
            String.format("0x%X (%d)", extendedValue, extendedValue)
        );

        StepInfo extractor_shiftleft2 = new StepInfo(
            "[Extractor]: EXTRACTOR -> SHIFT_LEFT_2",
            ComponentID.EXTRACTOR, ComponentID.SHIFT_LEFT_2, BusID.Extractor_ShiftLeft2,
            String.format("0x%X (%d)", extendedValue, extendedValue)
        );

        microSteps.add(new MicroStep(
            Set.of(extractor_alu, extractor_shiftleft2),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return extendedValue;
    }

    private long muxAluSrc_execute(long muxAlu_0, long muxAlu_1, char aluSrc) {
        if (aluSrc == 'x') return -1;

        long selectedValue = (aluSrc == '1') ? muxAlu_1 : muxAlu_0;
        StepInfo muxAluSrc = new StepInfo(
            "[MuxAlu_Alu]: MUX_ALUSrc -> ALU",
            ComponentID.MUX_ALUSrc, ComponentID.ALU, BusID.MuxAlu_Alu,
            String.format("0x%X (%d)", selectedValue, selectedValue)
        );

        microSteps.add(new MicroStep(
            Set.of(muxAluSrc),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return selectedValue;
    }

    private int aluControl_execute(ControlSignals controlSignals) {
        if (controlSignals.aluOp() == 404) {
            return 404; // No ALU operation defined
        }

        StepInfo aluControl = new StepInfo(
            "[ALUControl]: ALU_CONTROL -> ALU",
            ComponentID.ALU_CONTROL, ComponentID.ALU, BusID.AluControl_Alu_Signal,
            String.format("%4s", Integer.toBinaryString(controlSignals.operation())).replace(' ', '0')
        );

        microSteps.add(new MicroStep(
            Set.of(aluControl),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return controlSignals.operation(); // Return the ALU operation code
    }

    private long alu_execute(long alu_in_a, long alu_in_b, int operation, ControlSignals controlSignals, String mnemonic) {
        if (operation == 404) {
            return 0;
        }

        Set<StepInfo> aluSteps = new HashSet<>();

        ALUResult aluResult = new ArithmeticLogicUnit().execute(alu_in_a, alu_in_b, operation);
        if (controlSignals.flagWrite() == '1') {
            flagN = aluResult.negativeFlag();
            flagZ = aluResult.zeroFlag();
            flagC = aluResult.carryFlag();
            flagV = aluResult.overflowFlag();

            StepInfo alu_FlagN = new StepInfo(
                "[ALU_FlagN]: ALU -> N_FLAG",
                ComponentID.ALU, ComponentID.N_FLAG, BusID.Alu_NFlag,
                String.valueOf(flagN)
            );
            StepInfo alu_FlagZ = new StepInfo(
                "[ALU_FlagZ]: ALU -> Z_FLAG",
                ComponentID.ALU, ComponentID.Z_FLAG, BusID.Alu_ZFlag,
                String.valueOf(flagZ)
            );
            StepInfo alu_FlagC = new StepInfo(
                "[ALU_FlagC]: ALU -> C_FLAG",
                ComponentID.ALU, ComponentID.C_FLAG, BusID.Alu_CFlag,
                String.valueOf(flagC)
            );
            StepInfo alu_FlagV = new StepInfo(
                "[ALU_FlagV]: ALU -> V_FLAG",
                ComponentID.ALU, ComponentID.V_FLAG, BusID.Alu_VFlag,
                String.valueOf(flagV)
            );
            aluSteps.add(alu_FlagN);
            aluSteps.add(alu_FlagZ);
            aluSteps.add(alu_FlagC);
            aluSteps.add(alu_FlagV);
        }

        if (controlSignals.uncondBranch() == '1') {
            if (mnemonic.equals("CBZ")) {
                StepInfo alu_brZeroAnd = new StepInfo(
                    "[Alu_BrZeroAnd]: ALU -> BR_ZERO_AND",
                    ComponentID.ALU, ComponentID.BR_ZERO_AND, BusID.Alu_BrZeroAnd,
                    String.format("CBZ (Rt == 0?): %d", + (aluResult.result() == 0 ? 1 : 0))
                );
                aluSteps.add(alu_brZeroAnd);
            } else if (mnemonic.equals("CBNZ")) {
                StepInfo alu_brZeroAnd = new StepInfo(
                    "[Alu_BrZeroAnd]: ALU -> BR_ZERO_AND",
                    ComponentID.ALU, ComponentID.BR_ZERO_AND, BusID.Alu_BrZeroAnd,
                    String.format("CBNZ (Rt != 0?): %d", + (aluResult.result() != 0 ? 1 : 0))
                );
                aluSteps.add(alu_brZeroAnd);
            }
        }

        StepInfo alu_dataMemory = new StepInfo(
            "[ALU_DataMemory]: ALU -> DATA_MEMORY",
            ComponentID.ALU, ComponentID.DATA_MEMORY, BusID.Alu_DataMemory,
            String.format("0x%X (%d)", aluResult.result(), aluResult.result())
        );
        aluSteps.add(alu_dataMemory);

        StepInfo alu_muxWbRegFile = new StepInfo(
            "[ALU_MuxWbRegFile]: ALU -> MUX_WB_REGFILE",
            ComponentID.ALU, ComponentID.MUX_WB_REGFILE, BusID.Alu_MuxWbRegFile_0,
            String.format("0x%X (%d)", aluResult.result(), aluResult.result())
        );
        aluSteps.add(alu_muxWbRegFile); 

        microSteps.add(new MicroStep(
            aluSteps,
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return aluResult.result();
    }

    private long dataMemory_execute(long address, long writeData, ControlSignals controlSignals, String mnemonic) {
        if (controlSignals.memRead() == '0' && controlSignals.memWrite() == '0') {
            return 0; // No memory operation
        }

        boolean isWrite = controlSignals.memWrite() == '1';
        boolean isRead = controlSignals.memRead() == '1';

        long readValue = 0;
        if (mnemonic.equals("LDUR") || mnemonic.equals("STUR")) {
            readValue = memoryController.accessMemory_doubleWord(address, writeData, isWrite, isRead);
        } else if (mnemonic.equals("LDURSW") || mnemonic.equals("STURW")) {
            readValue = memoryController.accessMemory_word(address, writeData, isWrite, isRead) & 0xFFFFFFFFL; // Ensure 32-bit word access
        } else if (mnemonic.equals("LDURH") || mnemonic.equals("STURH")) {
            readValue = memoryController.accessMemory_halfword(address, writeData, isWrite, isRead) & 0xFFFFL; // Ensure 16-bit halfword access
        } else if (mnemonic.equals("LDURB") || mnemonic.equals("STURB")) {
            readValue = memoryController.accessMemory_byte(address, writeData, isWrite, isRead) & 0xFFL; // Ensure 8-bit byte access
        }

        if (isRead) {
            StepInfo dataMem_muxWbReg = new StepInfo(
                "[DataMemory_MuxWbRegFile_1]: DATA_MEMORY -> MUX_WB_REGFILE",
                ComponentID.DATA_MEMORY, ComponentID.MUX_WB_REGFILE, BusID.DataMemory_MuxWbRegFile_1,
                String.format("0x%X (%d)", readValue, readValue)
            );
            microSteps.add(new MicroStep(
                Set.of(dataMem_muxWbReg),
                memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
            ));
        }

        return readValue;
    }

    private long muxWriteBack_execute(long muxWbReg_0, long muxWbReg_1, char memToReg) {
        if (memToReg == 'x') return -1;

        long writeBackValue = (memToReg == '1') ? muxWbReg_1 : muxWbReg_0;
        StepInfo muxWbRegFile = new StepInfo(
            "[MuxWbRegFile]: MUX_WB_REGFILE -> REGISTERS_FILE",
            ComponentID.MUX_WB_REGFILE, ComponentID.REGISTERS_FILE, BusID.MuxWbRegFile_RegFile,
            String.format("0x%X (%d)", writeBackValue, writeBackValue)
        );

        microSteps.add(new MicroStep(
            Set.of(muxWbRegFile),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return writeBackValue;
    }

    private boolean flagBranch_execute(Instruction instruction, char flagBranch) {
        Objects.requireNonNull(instruction, "Instruction cannot be null.");
        boolean isBranchTaken = false;
        if (flagBranch == '1') {        
            FlagBranchControl.FlagControl flagBranchControl = FlagBranchControl.getBranchCond(flagZ, flagV, flagN, flagC, instruction.getCond_CB());
            
            StepInfo flagN_out = new StepInfo(
                "[Flag_BrFlagAnd]: FLAG -> BR_FLAG_AND",
                ComponentID.N_FLAG, ComponentID.BR_FLAG_AND, BusID.NFlag_BrFlagAnd,
                flagBranchControl.message()
            );
            StepInfo flagZ_out = new StepInfo("", ComponentID.Z_FLAG, ComponentID.BR_FLAG_AND, BusID.ZFlag_BrFlagAnd, "");
            StepInfo flagC_out = new StepInfo("", ComponentID.C_FLAG, ComponentID.BR_FLAG_AND, BusID.CFlag_BrFlagAnd, "");
            StepInfo flagV_out = new StepInfo("", ComponentID.V_FLAG, ComponentID.BR_FLAG_AND, BusID.VFlag_BrFlagAnd, "");

            microSteps.add(new MicroStep(
                Set.of(flagN_out, flagZ_out, flagC_out, flagV_out),
                memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
            ));

            isBranchTaken = flagBranchControl.result();
        }

        StepInfo brFlagAnd_brOr = new StepInfo(
            "[BrFlagAnd_BrOr]: BR_FLAG_AND -> BR_OR",
            ComponentID.BR_FLAG_AND, ComponentID.BR_OR, BusID.BrFlagAnd_BrOr,
            isBranchTaken ? "1" : "0"
        );
        microSteps.add(new MicroStep(
            Set.of(brFlagAnd_brOr),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return isBranchTaken;
    }

    private boolean zeroBranch_execute(long aluRes, String mnemonic, char zeroBranch) {       
        boolean isBranchTaken = false;
        if (zeroBranch == '1') {
            if (mnemonic.equals("CBZ")) {
                isBranchTaken = (aluRes == 0);
            } else if (mnemonic.equals("CBNZ")) {
                isBranchTaken = (aluRes != 0);
            }
        }

        StepInfo uncondBranch_brOr = new StepInfo(
            "[BrZeroAnd_BrOr]: BR_ZERO_AND -> BR_OR",
            ComponentID.BR_ZERO_AND, ComponentID.BR_OR, BusID.BrZeroAnd_BrOr,
            isBranchTaken ? "1" : "0"
        );

        microSteps.add(new MicroStep(
            Set.of(uncondBranch_brOr),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return isBranchTaken;
    }

    private boolean brOr_execute(boolean isBranchTaken) {
        StepInfo brOr = new StepInfo(
            "[BrOr_MuxPCSrc_Signal]: BR_OR -> MUX_PCSrc",
            ComponentID.BR_OR, ComponentID.MUX_PCSrc, BusID.BrOr_MuxPCSrc_Signal,
            isBranchTaken ? "1" : "0"
        );

        microSteps.add(new MicroStep(
            Set.of(brOr),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return isBranchTaken;
    }

    private long shiftLeft2_execute(long value) {
        long shiftedValue = value << 2; 
        
        StepInfo shiftLeft2 = new StepInfo(
            "[ShiftLeft2_BranchAdder]: SHIFT_LEFT_2 -> BR_ADDER",
            ComponentID.SHIFT_LEFT_2, ComponentID.BR_ADDER, BusID.ShiftLeft2_BranchAdder,
            String.format("0x%X (%d)", shiftedValue, shiftedValue)
        );

        microSteps.add(new MicroStep(
            Set.of(shiftLeft2),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return shiftedValue;
    }

    private long branchAdder_execute(long address, long shiftedValue) {
        long res = address + shiftedValue; // Branch Adder logic

        StepInfo branchAdder = new StepInfo(
            "[BranchAdder_MuxPCSrc_1]: BR_ADDER -> MUX_PCSrc",
            ComponentID.BR_ADDER, ComponentID.MUX_PCSrc, BusID.BranchAdder_MuxPCSrc_1,
            String.format("0x%X (%d)", res, res)
        );

        microSteps.add(new MicroStep(
            Set.of(branchAdder),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return res; // Return the result of the branch adder
    }

    private long adder4_execute(long address) {
        long nextPC = address + 4; // Adder4 logic

        StepInfo adder4 = new StepInfo(
            "[Adder4_MuxPCSrc_0]: PC_ADDER4 -> MUX_PCSrc",
            ComponentID.PC_ADDER4, ComponentID.MUX_PCSrc, BusID.PCAdder4_MuxPCSrc_0,
            String.format("0x%X (%d)", nextPC, nextPC)
        );

        microSteps.add(new MicroStep(
            Set.of(adder4),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return nextPC; // Return the next program counter value
    }

    private long muxPCSrc_execute(long branchAddress, long nextPC, boolean isBranch) {
        long newPC = (isBranch) ? branchAddress : nextPC; // Mux PC Src logic

        StepInfo muxPCSrc = new StepInfo(
            "[MuxPCSrc]: MUX_PCSrc -> PROGRAM_COUNTER",
            ComponentID.MUX_PCSrc, ComponentID.PROGRAM_COUNTER, BusID.MuxPCSrc_ProgramCounter,
            String.format("0x%X", newPC)
        );

        microSteps.add(new MicroStep(
            Set.of(muxPCSrc),
            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()
        ));

        return newPC; // Return the new program counter value
    }

    // --- Core Simulation Logic ---
    /**
     * Executes a single micro-step of the simulation, updating the internal state
     * and returning a list of micro-steps representing the current state of the
     * simulator.
     * @return A list of micro-steps representing the current state of the simulator.
     * @throws SimulationException If an error occurs during simulation.
     */
    public void step() throws SimulationException {
        microSteps.clear();
        
        try {
            // Step 1
            long currentPC = PC_Output();

            // Step 2:
            Instruction currentInstruction = IMEM_execute(currentPC);
            if (currentInstruction == null) {
                throw new SimulationException("No instruction fetched at PC: " + Long.toHexString(programCounter.getCurrentAddress()), null, programCounter.getCurrentAddress());
            }
            InstructionDefinition definition = currentInstruction.getDefinition();

            // Step 3: 
            BitSet instructionBitSet = splitInstruction(currentInstruction);

            // Step 4: 
            ControlSignals controlSignals = controlUnit_execute(currentInstruction);

            // Step 5:
            int muxReg_0 = Instruction.extractBits(instructionBitSet, 16, 20);
            int muxReg_1 = Instruction.extractBits(instructionBitSet, 0, 4);
            int selectedReg = muxRecFile_execute(muxReg_0, muxReg_1, controlSignals.reg2Loc());

            // Step 6:
            int readReg_1 = Instruction.extractBits(instructionBitSet, 5, 9);
            int readReg_2 = selectedReg;
            long[] regValues = read_Regfile(readReg_1, readReg_2);

            // Step 7: Extractor
            long extendedValue = extractor_execute(instructionBitSet, definition.getFormat(), definition.getMnemonic());

            // Step 8: Mux ALU Src
            long muxAlu_0 = regValues[1]; // Value from readReg_1
            long muxAlu_1 = extendedValue; // Value from extractor
            long muxAluSrc_out = muxAluSrc_execute(muxAlu_0, muxAlu_1, controlSignals.aluSrc());

            // Step 9: ALU Control
            int operand = aluControl_execute(controlSignals);
            
            // Step 10: ALU Execution
            long aluResult = -1;
            if (operand != 404) {
                long alu_in_a = regValues[0]; // Value from readReg_1
                long alu_in_b = muxAluSrc_out; // Value from Mux ALU Src
                aluResult = alu_execute(alu_in_a, alu_in_b, operand, controlSignals, definition.getMnemonic());

                long muxWbReg_0 = aluResult; // ALU result
                long muxWbReg_1 = 0;
                // Step 11:
                if (controlSignals.memWrite() == '1' || controlSignals.memRead() == '1') {
                    long writeData = regValues[1]; // Value from readReg_2
                    long address = aluResult;

                    long readData = dataMemory_execute(address, writeData, controlSignals, definition.getMnemonic());

                    // Step 12: Mux WB RegFile
                    muxWbReg_1 = readData; // Data memory read value
                    
                }
                long writeBackValue = muxWriteBack_execute(muxWbReg_0, muxWbReg_1, controlSignals.memToReg());
                if (controlSignals.regWrite() == '1') {
                    if (definition.getMnemonic().equals("MOVK")) {
                        int rd = Instruction.extractBits(instructionBitSet, 0, 4);
                        long currentValue = registerController.readRegister(rd);
                        int hw = Instruction.extractBits(instructionBitSet, 21, 22);
                        int shift = hw * 16;

                        long imm16 = Instruction.extractBits(instructionBitSet, 5, 20) & 0xFFFFL;
                        long mask = ~(0xFFFFL << shift);
                        long shifted = imm16 << shift;

                        writeBackValue = (currentValue & mask) | shifted;
                    }

                    registerController.writeRegister(Instruction.extractBits(instructionBitSet, 0, 4), writeBackValue, true);
                }
            }

            // Step 13
            boolean isBranch = flagBranch_execute(currentInstruction, controlSignals.flagBranch());

            // Step 14:
            if (operand != 404) 
                if (zeroBranch_execute(aluResult, definition.getMnemonic(), controlSignals.zeroBranch())) isBranch = true;

            // Step 15:
            if (controlSignals.uncondBranch() == '1') isBranch = true;

            brOr_execute(isBranch);

            // Step 16: Shift Left 2
            long shiftLeft2Value = shiftLeft2_execute(extendedValue);

            // Step 17: Branch Adder
            long branchAddress = branchAdder_execute(currentPC, shiftLeft2Value);

            // Step 18: Adder4
            long nextPC = adder4_execute(currentPC);

            // Step 19: Mux PC Src
            long finalPC = muxPCSrc_execute(branchAddress, nextPC, isBranch);
            programCounter.setAddress(finalPC);; // Update the Program Counter

        } catch (Exception e) {
            throw new SimulationException("Error during Program Counter step: " + e.getMessage(), e, programCounter.getCurrentAddress());
        } 
    }



    // --- Getters & Setters ---

    public List<MicroStep> getMicroSteps() throws SimulationException {
        step();
        return new ArrayList<>(microSteps);
        // System.out.print
    }

    public List<MicroStep> getMicroStepsWithoutStep() {
        return new ArrayList<>(microSteps);
    }
    
    /**
     * Returns a string representation (disassembly and bytecode) of the instruction
     * currently pointed to by the Program Counter (PC).
     * Handles potential errors during instruction fetch.
     * @return Disassembled instruction string, or an error message if fetch fails.
     */
    public String getCurrentInstruction() {
        long currentPC = programCounter.getCurrentAddress();
        Instruction instruction = instructionMemory.fetch(currentPC);
        return instruction != null ? instruction.toString() : "No instruction at PC: " + Long.toHexString(currentPC);
    }

    /**
     * Loads a list of assembled instructions into the engine's InstructionMemory
     * and resets the engine's state (PC, registers, memory, flags).
     * @param instructions The list of Instruction objects to load. Must not be null or empty.
     * @throws IllegalArgumentException if the provided instruction list is null or empty.
     */
    public void loadInstructions(List<Instruction> instructions) {
        if (instructions == null || instructions.isEmpty()) {
            throw new IllegalArgumentException("Instruction list cannot be null or empty.");
        }
        reset();
        System.out.println(ColoredLog.PENDING + "Loading instructions into Instruction Memory...");
        instructionMemory.loadInstructions(instructions);
    }

    /**
     * Gets the InstructionMemory component used by this engine.
     * @return The InstructionMemory instance.
     */
    public InstructionMemory getInstructionMemory() {
        return instructionMemory;
    }

    /**
     * Gets the DataMemoryController component used by this engine.
     * @return The DataMemoryController instance.
     */
    public DataMemoryController getDataMemoryController() {
        return memoryController;
    }

    /**
     * Gets the RegisterFileController component used by this engine.
     * @return The RegisterFileController instance.
     */
    public RegisterFileController getRegisterController() {
        return registerController;
    }

    /**
     * Gets the ProgramCounter component used by this engine.
     * @return The ProgramCounter instance.
     */
    public ProgramCounter getProgramCounter() {
        return programCounter;
    }
    
    /**
     * Checks if the simulator is currently in a halted state (due to error,
     * infinite loop detection, or potentially a HALT instruction).
     * @return true if the simulator is halted, false otherwise.
     */
    // public boolean isHalted() {
    //     return halted;
    // }


    // --- Debugging & Helpers ---
    /**
     * Displays the current internal state of the simulator (PC, registers, flags, cycle count)
     * to the standard output console. Primarily used for debugging.
     */
    public void displayState() {
        System.out.println("\n--- Simulator State ---");
        // System.out.printf("Cycle Count: %d\n", cycleCount);
        System.out.printf("PC: 0x%X\n", programCounter.getCurrentAddress());
        System.out.println("Registers: " + registerController.getStorage().toString());
        System.out.println("Flags: N=" + flagN + ", Z=" + flagZ + ", C=" + flagC + ", V=" + flagV);
        System.out.println("Data Memory: " + memoryController.getStorage().toString());
        System.out.println("------------------------");
    }


}
