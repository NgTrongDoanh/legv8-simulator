/**
 * @author TrDoanh
 * @version 1.0 --- Maybe exist bugs :)
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
    private final ArithmeticLogicUnit alu;
    private final DataMemoryController memoryController;
    
    // --- Simulation State ---
    private long cycleCount = 0;
    private boolean halted = false;
    private boolean externalHaltRequest = false; 
    
    // --- Processor Flags ---
    private boolean flagN = false; 
    private boolean flagZ = false; 
    private boolean flagC = false; 
    private boolean flagV = false; 

    
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
        this.alu = new ArithmeticLogicUnit();
        this.memoryController = new DataMemoryController(new MemoryStorage());
        
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
        this.alu = new ArithmeticLogicUnit();
        this.memoryController = Objects.requireNonNull(memoryController, "DataMemoryController cannot be null.");

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
        this.alu = new ArithmeticLogicUnit();
        this.memoryController = new DataMemoryController(new MemoryStorage());
        
        System.out.println(ColoredLog.SUCCESS + "LEGv8 CPU Simulator Engine initialized.");
        resetState();
    }

    
    // --- State Management ---

    /**
     * Resets the internal simulator state variables (cycle count, flags, halted status).
     * Called by the public reset() method.
     */
    private void resetState() {
        cycleCount = 0;
        halted = false;
        externalHaltRequest = false;
        flagN = false;
        flagZ = false;
        flagC = false;
        flagV = false;
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


    // --- Core Simulation Logic ---
    /**
     * Executes a single micro-step of the simulation, updating the internal state
     * and returning a list of micro-steps representing the current state of the
     * simulator.
     * @return A list of micro-steps representing the current state of the simulator.
     * @throws SimulationException If an error occurs during simulation.
     */
    public List<MicroStep> stepAndGetMicroSteps() throws SimulationException {
        if (halted || externalHaltRequest) {
            halted = true;
            System.out.println(ColoredLog.WARNING + "Simulator::stepAndGetMicroSteps() - Already halted or external request.");
            return Collections.emptyList(); 
        }

        List<MicroStep> microSteps = new ArrayList<>();
        long currentPC = programCounter.getCurrentAddress();
       
        Instruction currentInstruction = null;
        BitSet instructionBits = null; 
        InstructionDefinition definition = null;
        String mnemonic = "???"; char format = '?';
        long imem_in = 0; 
        ControlSignals controlSignals = ControlSignals.NOP; 
        int controlSignals_in;
        int muxReg_0 = 0, muxReg_1 = 0;
        int regfile_readReg1 = 0, regfile_readReg2 = 0;
        int regfile_writeReg = 0;
        long regfile_writeData = 0;
        int extractor_in = 0;
        long alu_in_a = 0, alu_in_b = 0;
        ALUResult aluRes;
        long muxAlu_0 = 0, muxAlu_1 = 0;
        int aluCtrl_in = 0, aluCtrl_out = 0;
        long dataMem_Addr = 0, dataMem_writeData = 0;
        long muxWbReg_0 = 0, muxWbReg_1 = 0;
        long shiftLeft2_in = 0;
        long adder4_in = 0;
        long brAdder_in_a = 0, brAdder_in_b = 0;
        long MuxPCSrc_0 = 0, MuxPCSrc_1 = 0;
        boolean brZeroAnd_in = false;
        boolean brOr_in_brFlagAnd = false, brOr_in_brZeroAnd = false;
        long nextPC = 0;

        try {
            // Micro-step 1:
            imem_in = adder4_in = brAdder_in_a = currentPC;
            
            StepInfo pc_adder4 = new StepInfo("[ProgramCounter_PCAdder4]: PROGRAM_COUNTER -> PC_ADDER4", 
                                            ComponentID.PROGRAM_COUNTER, ComponentID.PC_ADDER4, BusID.ProgramCounter_PCAdder4,
                                            "0x" + Long.toHexString(adder4_in));
            StepInfo pc_branch = new StepInfo("[ProgramCounter_BranchAdder]: PROGRAM_COUNTER -> BR_ADDER",
                                            ComponentID.PROGRAM_COUNTER, ComponentID.BR_ADDER, BusID.ProgramCounter_BranchAdder,
                                            "0x" + Long.toHexString(brAdder_in_a));
            StepInfo pc_imem = new StepInfo("[ProgramCounter_InstructionMemory]: PROGRAM_COUNTER -> INSTRUCTION_MEMORY", 
                                            ComponentID.PROGRAM_COUNTER, ComponentID.INSTRUCTION_MEMORY, BusID.ProgramCounter_InstructionMemory, 
                                            "0x" + Long.toHexString(imem_in));
            microSteps.add(new MicroStep(Set.of(pc_imem, pc_adder4, pc_branch),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 2:
            currentInstruction = instructionMemory.fetch(currentPC); 
            instructionBits = currentInstruction.getBytecode();
            definition = currentInstruction.getDefinition();
            mnemonic = definition.getMnemonic();
            format = definition.getFormat();            

            StepInfo imem_sp = new StepInfo("[InstructionMemory_Splitter]: INSTRUCTION_MEMORY -> SPLITTER", 
                                            ComponentID.INSTRUCTION_MEMORY, ComponentID.SPLITTER, BusID.InstructionMemory_Splitter, 
                                            currentInstruction.toString());
            microSteps.add(new MicroStep(Set.of(imem_sp),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 3:
            controlSignals_in = Instruction.extractBits(instructionBits, 21, 31);
            controlSignals = definition.getControlSignals();
            regfile_readReg1 = Instruction.extractBits(instructionBits, 5, 9);
            muxReg_0 = Instruction.extractBits(instructionBits, 16, 20);
            muxReg_1 = Instruction.extractBits(instructionBits, 0, 4);
            regfile_writeReg = Instruction.extractBits(instructionBits, 0, 4);
            extractor_in = Instruction.extractBits(instructionBits, 0, 31);
            aluCtrl_in = Instruction.extractBits(instructionBits, 21, 31);
            
            StepInfo splitter_ctrl = new StepInfo("[Splitter_ControlUnit]: SPLITTER -> CONTROL_UNIT", 
                                            ComponentID.SPLITTER, ComponentID.CONTROL_UNIT, BusID.Splitter_ControlUnit, 
                                            String.format("%11s", Integer.toBinaryString(controlSignals_in)).replace(' ', '0'));       
            StepInfo sp_muxregfile_0 = new StepInfo("[Splitter_MuxRegFile_0]: SPLITTER -> MUX_REGFILESrc",
                                            ComponentID.SPLITTER, ComponentID.MUX_REGFILESrc, BusID.Splitter_MuxRegFile_0, 
                                            String.format("%5s", Integer.toBinaryString(muxReg_0)).replace(' ', '0'));
            StepInfo sp_muxregfile_1 = new StepInfo("[Splitter_MuxRegFile_1]: SPLITTER -> MUX_REGFILESrc",
                                            ComponentID.SPLITTER, ComponentID.MUX_REGFILESrc, BusID.Splitter_MuxRegFile_1, 
                                            String.format("%5s", Integer.toBinaryString(muxReg_1)).replace(' ', '0'));
            StepInfo sp_regfile1 = new StepInfo("[Splitter_ReadReg1]: SPLITTER -> REGISTERS_FILE",
                                            ComponentID.SPLITTER, ComponentID.REGISTERS_FILE, BusID.Splitter_RegFile1, 
                                            String.format("%5s", Integer.toBinaryString(regfile_readReg1)).replace(' ', '0'));
            StepInfo sp_regfile2 = new StepInfo("[Splitter_WriteReg]: SPLITTER -> REGISTERS_FILE",
                                            ComponentID.SPLITTER, ComponentID.REGISTERS_FILE, BusID.Splitter_RegFile2, 
                                            String.format("%5s", Integer.toBinaryString(regfile_writeReg)).replace(' ', '0'));
            StepInfo sp_signextend = new StepInfo("[Splitter_Extractor]: SPLITTER -> EXTRACTOR",
                                            ComponentID.SPLITTER, ComponentID.EXTRACTOR, BusID.Splitter_Extractor, 
                                            String.format("%32s", Integer.toBinaryString(extractor_in)).replace(' ', '0'));
            StepInfo sp_aluCtrl = new StepInfo("[Splitter_AluControl]: SPLITTER -> ALU_CONTROL",
                                            ComponentID.SPLITTER, ComponentID.ALU_CONTROL, BusID.Splitter_AluControl, 
                                            String.format("%11s", Integer.toBinaryString(aluCtrl_in)).replace(' ', '0'));
            microSteps.add(new MicroStep(Set.of(splitter_ctrl, sp_muxregfile_0, sp_muxregfile_1, 
                                            sp_regfile1, sp_regfile2, sp_signextend, sp_aluCtrl),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 4:
            controlSignals = definition.getControlSignals();
            Set<StepInfo> ctrl_unit = new HashSet<>();
            
            StepInfo ctrl_muxRegfile = new StepInfo("[ControlUnit_MuxRegFile_Signal_Reg2Loc]: CONTROL_UNIT -> MUX_REGFILESrc",
                                            ComponentID.CONTROL_UNIT, ComponentID.MUX_REGFILESrc, BusID.ControlUnit_MuxRegFile_Signal_Reg2Loc, 
                                            controlSignals.reg2Loc() ? "1" : "0");
            ctrl_unit.add(ctrl_muxRegfile);
            StepInfo ctrl_brOr = new StepInfo("[ControlUnit_BrOr_Signal_UncondBranch]: CONTROL_UNIT -> BR_OR",
                                            ComponentID.CONTROL_UNIT, ComponentID.BR_OR, BusID.ControlUnit_BrOr_Signal_UncondBranch, 
                                            controlSignals.uncondBranch() ? "1" : "0");
            ctrl_unit.add(ctrl_brOr);
            StepInfo ctrl_brFlag = new StepInfo("[ControUnit_BrFlagAnd_Signal_FlagBranch]: CONTROL_UNIT -> BR_FLAG_AND",
                                            ComponentID.CONTROL_UNIT, ComponentID.BR_FLAG_AND, BusID.ControlUnit_BrFlagAnd_Signal_FlagBranch, 
                                            controlSignals.flagBranch() ? "1" : "0");
            ctrl_unit.add(ctrl_brFlag);
            StepInfo ctrl_brZero = new StepInfo("[ControlUnit_BrZeroAnd_Signal_ZeroBranch]: CONTROL_UNIT -> BR_ZERO_AND",
                                            ComponentID.CONTROL_UNIT, ComponentID.BR_ZERO_AND, BusID.ControlUnit_BrZeroAnd_Signal_ZeroBranch, 
                                            controlSignals.zeroBranch() ? "1" : "0");
            ctrl_unit.add(ctrl_brZero);
            StepInfo ctrl_memRead = new StepInfo("[ControlUnit_DataMemory_Signal_MemRead]: CONTROL_UNIT -> DATA_MEMORY",
                                            ComponentID.CONTROL_UNIT, ComponentID.DATA_MEMORY, BusID.ControlUnit_DataMemory_Signal_MemRead, 
                                            controlSignals.memRead() ? "1" : "0");
            ctrl_unit.add(ctrl_memRead);
            StepInfo ctrl_memToReg = new StepInfo("[ControlUnit_MuxWbRegFile_Signal_MemToReg]: CONTROL_UNIT -> MUX_WB_REGFILE",     
                                            ComponentID.CONTROL_UNIT, ComponentID.MUX_WB_REGFILE, BusID.ControlUnit_MuxWbRegFile_Signal_MemToReg, 
                                            controlSignals.memToReg() ? "1" : "0");
            ctrl_unit.add(ctrl_memToReg);
            StepInfo ctrl_memWrite = new StepInfo("[ControlUnit_DataMemory_Signal_MemWrite]: CONTROL_UNIT -> DATA_MEMORY",
                                            ComponentID.CONTROL_UNIT, ComponentID.DATA_MEMORY, BusID.ControlUnit_DataMemory_Signal_MemWrite, 
                                            controlSignals.memWrite() ? "1" : "0");
            ctrl_unit.add(ctrl_memWrite);

            StepInfo ctrl_flagWrite_n = new StepInfo("[ControlUnit_Flags_Signal_FlagWrite]: CONTROL_UNIT -> FLAGS",
                                            ComponentID.CONTROL_UNIT, ComponentID.N_FLAG, BusID.ControlUnit_Flags_Signal_FlagWrite, 
                                            controlSignals.flagWrite() ? "1" : "0");
            ctrl_unit.add(ctrl_flagWrite_n);
            StepInfo ctrl_flagWrite_z = new StepInfo("[ControlUnit_Flags_Signal_FlagWrite]: CONTROL_UNIT -> FLAGS",
                                            ComponentID.CONTROL_UNIT, ComponentID.Z_FLAG, BusID.ControlUnit_Flags_Signal_FlagWrite, 
                                            controlSignals.flagWrite() ? "1" : "0");
            ctrl_unit.add(ctrl_flagWrite_z);
            StepInfo ctrl_flagWrite_c = new StepInfo("[ControlUnit_Flags_Signal_FlagWrite]: CONTROL_UNIT -> FLAGS",
                                            ComponentID.CONTROL_UNIT, ComponentID.C_FLAG, BusID.ControlUnit_Flags_Signal_FlagWrite, 
                                            controlSignals.flagWrite() ? "1" : "0");
            ctrl_unit.add(ctrl_flagWrite_c);
            StepInfo ctrl_flagWrite_v = new StepInfo("[ControlUnit_Flags_Signal_FlagWrite]: CONTROL_UNIT -> FLAGS",
                                            ComponentID.CONTROL_UNIT, ComponentID.V_FLAG, BusID.ControlUnit_Flags_Signal_FlagWrite, 
                                            controlSignals.flagWrite() ? "1" : "0");
            ctrl_unit.add(ctrl_flagWrite_v);
            StepInfo ctrl_aluSrc = new StepInfo("[ControlUnit_MuxAlu_Signal_AluSrc]: CONTROL_UNIT -> MUX_ALUSrc",
                                            ComponentID.CONTROL_UNIT, ComponentID.MUX_ALUSrc, BusID.ControlUnit_MuxAlu_Signal_AluSrc, 
                                            controlSignals.aluSrc() ? "1" : "0");
            ctrl_unit.add(ctrl_aluSrc);
            if (controlSignals.aluOp() != 404) {
                StepInfo ctrl_aluOp = new StepInfo("[ControlUnit_AluControl_Signal_AluOp]: CONTROL_UNIT -> ALU_CONTROL",
                                            ComponentID.CONTROL_UNIT, ComponentID.ALU_CONTROL, BusID.ControlUnit_AluControl_Signal_AluOp, 
                                            String.format("%4s", Integer.toBinaryString(controlSignals.aluOp())).replace(' ', '0'));
                ctrl_unit.add(ctrl_aluOp);
            }
            StepInfo ctrl_regWrite = new StepInfo("[ControlUnit_RegFile_Signal_RegWrite]: CONTROL_UNIT -> REGISTERS_FILE",
                                            ComponentID.CONTROL_UNIT, ComponentID.REGISTERS_FILE, BusID.ControlUnit_RegFile_Signal_RegWrite, 
                                            controlSignals.regWrite() ? "1" : "0");
            ctrl_unit.add(ctrl_regWrite);
            microSteps.add(new MicroStep(ctrl_unit,
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 5:
            regfile_readReg2 = controlSignals.reg2Loc() ? muxReg_1 : muxReg_0;
            
            StepInfo muxRegfile_out = new StepInfo("[MuxRegFile_RegFile]: MUX_REGFILE -> REGISTERS_FILE",
                                            ComponentID.MUX_REGFILESrc, ComponentID.REGISTERS_FILE, BusID.MuxRegFile_RegFile, 
                                            String.format("%5s", Integer.toBinaryString(regfile_readReg2)).replace(' ', '0'));
            microSteps.add(new MicroStep(Set.of(muxRegfile_out),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 6:
            alu_in_a = registerController.readRegister((int)regfile_readReg1);
            dataMem_writeData = muxAlu_0 = registerController.readRegister((int)regfile_readReg2);

            StepInfo regfile_alu = new StepInfo("[RegFile_Alu]: REGISTERS_FILE -> ALU", 
                                            ComponentID.REGISTERS_FILE, ComponentID.ALU, BusID.RegFile_Alu, 
                                            "0x" + Long.toHexString(alu_in_a) + "(" + alu_in_a + ")");
            StepInfo regfile_muxAlu = new StepInfo("[RegFile_MuxAlu_0]: REGISTERS_FILE -> MUX_ALUSrc",
                                            ComponentID.REGISTERS_FILE, ComponentID.MUX_ALUSrc, BusID.RegFile_MuxAlu_0, 
                                            "0x" + Long.toHexString(muxAlu_0) + "(" + muxAlu_0 + ")");
            StepInfo regfile_dataMem = new StepInfo("[RegFile_DataMemory]: REGISTERS_FILE -> DATA_MEMORY",
                                            ComponentID.REGISTERS_FILE, ComponentID.DATA_MEMORY, BusID.RegFile_DataMemory, 
                                            "0x" + Long.toHexString(dataMem_writeData) + "(" + dataMem_writeData + " | 0b" + Long.toBinaryString(dataMem_writeData) + ")");
            microSteps.add(new MicroStep(Set.of(regfile_alu, regfile_muxAlu, regfile_dataMem),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 7:
            shiftLeft2_in = muxAlu_1 = Extractor.extractAndExtend(extractor_in, format, mnemonic);
            
            StepInfo extractor_alu = new StepInfo("[Extractor_MuxAlu_1]: EXTRACTOR -> MUX_ALUSrc",
                                            ComponentID.EXTRACTOR, ComponentID.MUX_ALUSrc, BusID.Extractor_MuxAlu_1, 
                                            "0x" + Long.toHexString(muxAlu_1) + "(" + muxAlu_1 + ")");
            StepInfo extractor_shift = new StepInfo("[Extractor_ShiftLeft2]: EXTRACTOR -> SHIFT_LEFT_2",
                                            ComponentID.EXTRACTOR, ComponentID.SHIFT_LEFT_2, BusID.Extractor_ShiftLeft2, 
                                            "0x" + Long.toHexString(shiftLeft2_in) + "(" + shiftLeft2_in + ")");
            microSteps.add(new MicroStep(Set.of(extractor_alu, extractor_shift),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));
            
            // Micro-step 8:
            alu_in_b = controlSignals.aluSrc() ? muxAlu_1 : muxAlu_0;
            
            StepInfo muxAlu_alu = new StepInfo("[MuxAlu_Alu]: MUX_ALUSrc -> ALU",
                                            ComponentID.MUX_ALUSrc, ComponentID.ALU, BusID.MuxAlu_Alu, 
                                            "0x" + Long.toHexString(alu_in_b) + "(" + alu_in_b + ")");
            microSteps.add(new MicroStep(Set.of(muxAlu_alu),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));
            
            // Micro-step 9: (May be skipped if ALUOp is not 404)
            if (controlSignals.aluOp() != 404) {
                aluCtrl_out = controlSignals.operation();
                StepInfo aluCtrl_alu = new StepInfo("[AluControl_Alu_Signal]: ALU_CONTROL -> ALU",
                                                ComponentID.ALU_CONTROL, ComponentID.ALU, BusID.AluControl_Alu_Signal, 
                                                String.format("%4s", Integer.toBinaryString(aluCtrl_out)).replace(' ', '0'));
                microSteps.add(new MicroStep(Set.of(aluCtrl_alu),   
                                            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));
            }
            
            // Micro-step 10:
            Set<StepInfo> aluMicroSteps = new HashSet<>();
            aluRes = alu.execute(alu_in_a, alu_in_b, controlSignals.operation());
            muxWbReg_0 = dataMem_Addr = aluRes.result();

            if (controlSignals.flagWrite()) {
                flagN = aluRes.negativeFlag();
                flagZ = aluRes.zeroFlag();
                flagC = aluRes.carryFlag();
                flagV = aluRes.overflowFlag();

                StepInfo alu_flagN = new StepInfo("[Alu_NFlag]: ALU -> N_FLAG",
                                            ComponentID.ALU, ComponentID.N_FLAG, BusID.Alu_NFlag, 
                                                flagN ? "1" : "0");
                StepInfo alu_flagZ = new StepInfo("[Alu_ZFlag]: ALU -> Z_FLAG",
                                            ComponentID.ALU, ComponentID.Z_FLAG, BusID.Alu_ZFlag, 
                                            flagZ ? "1" : "0");
                StepInfo alu_flagC = new StepInfo("[Alu_CFlag]: ALU -> C_FLAG",
                                            ComponentID.ALU, ComponentID.C_FLAG, BusID.Alu_CFlag, 
                                            flagC ? "1" : "0");
                StepInfo alu_flagV = new StepInfo("[Alu_VFlag]: ALU -> V_FLAG",
                                            ComponentID.ALU, ComponentID.V_FLAG, BusID.Alu_VFlag, 
                                            flagV ? "1" : "0");
                aluMicroSteps.add(alu_flagN);
                aluMicroSteps.add(alu_flagZ);
                aluMicroSteps.add(alu_flagC);
                aluMicroSteps.add(alu_flagV);
            }

            if (mnemonic.equals("CBZ")) {
                brZeroAnd_in = alu_in_b == 0;
                StepInfo alu_brZeroAnd = new StepInfo("[Alu_BrZroAnd]: ALU -> BR_ZERO_AND",
                                            ComponentID.ALU, ComponentID.BR_ZERO_AND, BusID.Alu_BrZeroAnd, 
                                            "CBZ (Rt == 0?): " + (brZeroAnd_in ? "1" : "0"));
                aluMicroSteps.add(alu_brZeroAnd);
            } else if (mnemonic.equals("CBNZ")) {
                brZeroAnd_in = alu_in_b != 0;
                StepInfo alu_brZeroAnd = new StepInfo("[Alu_BrZroAnd]: ALU -> BR_ZERO_AND",
                                            ComponentID.ALU, ComponentID.BR_ZERO_AND, BusID.Alu_BrZeroAnd, 
                                            "CBNZ (Rt != 0?): " + (brZeroAnd_in ? "1" : "0"));
                aluMicroSteps.add(alu_brZeroAnd);
            }

            StepInfo alu_dataMemory = new StepInfo("[Alu_DataMemory]: ALU -> DATA_MEMORY",
                                            ComponentID.ALU, ComponentID.DATA_MEMORY, BusID.Alu_DataMemory, 
                                            "0x" + Long.toHexString(dataMem_Addr) + "(" + dataMem_Addr + ")");
            aluMicroSteps.add(alu_dataMemory);
            StepInfo alu_muxWbReg = new StepInfo("[Alu_MuxWbRegFile_0]: ALU -> MUX_WB_REGFILE",
                                            ComponentID.ALU, ComponentID.MUX_WB_REGFILE, BusID.Alu_MuxWbRegFile_0, 
                                            "0x" + Long.toHexString(muxWbReg_0) + "(" + muxWbReg_0 + ")");
            aluMicroSteps.add(alu_muxWbReg);
            microSteps.add(new MicroStep(aluMicroSteps,
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));   

            // Micro-step 11:
            muxWbReg_1 = memoryController.accessMemory(dataMem_Addr, (int)dataMem_writeData, controlSignals.memWrite(), controlSignals.memRead());
            
            StepInfo dataMem_muxWbReg = new StepInfo("[DataMemory_MuxWbRegFile_1]: DATA_MEMORY -> MUX_WB_REGFILE",
                                            ComponentID.DATA_MEMORY, ComponentID.MUX_WB_REGFILE, BusID.DataMemory_MuxWbRegFile_1, 
                                            "0x" + Long.toHexString(muxWbReg_1) + "(" + muxWbReg_1 + ")");
            microSteps.add(new MicroStep(Set.of(dataMem_muxWbReg),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));
            
            // Micro-step 12:
            regfile_writeData = controlSignals.memToReg() ? muxWbReg_1 : muxWbReg_0;
            registerController.writeRegister((int)(regfile_writeReg), regfile_writeData, controlSignals.regWrite());

            StepInfo muxWbReg_regfile = new StepInfo("[MuxWbRegFile_RegFile]: MUX_WB_REGFILE -> REGISTERS_FILE",
                                            ComponentID.MUX_WB_REGFILE, ComponentID.REGISTERS_FILE, BusID.MuxWbRegFile_RegFile, 
                                            "0x" + Long.toHexString(regfile_writeData) + "(" + regfile_writeData + ")");
            microSteps.add(new MicroStep(Set.of(muxWbReg_regfile),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 13:
            FlagBranchControl.FlagControl flagControl = FlagBranchControl.getBranchCond(flagN, flagZ, flagC, flagV, (int)regfile_writeReg);

            StepInfo flagN_out = new StepInfo("[Flag_BrFlagAnd]: FLAG -> BR_FLAG_AND", 
                                            ComponentID.N_FLAG, ComponentID.BR_FLAG_AND, BusID.NFlag_BrFlagAnd, 
                                            flagControl.message());
            StepInfo flagZ_out = new StepInfo("", ComponentID.Z_FLAG, ComponentID.BR_FLAG_AND, BusID.ZFlag_BrFlagAnd, "");
            StepInfo flagC_out = new StepInfo("", ComponentID.C_FLAG, ComponentID.BR_FLAG_AND, BusID.CFlag_BrFlagAnd, "");
            StepInfo flagV_out = new StepInfo("", ComponentID.V_FLAG, ComponentID.BR_FLAG_AND, BusID.VFlag_BrFlagAnd, "");
            microSteps.add(new MicroStep(Set.of(flagN_out, flagZ_out, flagC_out, flagV_out),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 14:
            brOr_in_brFlagAnd = controlSignals.flagBranch() && flagControl.result();

            StepInfo brFlagAnd_brOr = new StepInfo("[BrFlagAnd_BrOr]: BR_FLAG_AND -> BR_OR", 
                                            ComponentID.BR_FLAG_AND, ComponentID.BR_OR, BusID.BrFlagAnd_BrOr, 
                                            brOr_in_brFlagAnd ? "1" : "0");
            microSteps.add(new MicroStep(Set.of(brFlagAnd_brOr),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 15:
            brOr_in_brZeroAnd = controlSignals.zeroBranch() && brZeroAnd_in;
            
            StepInfo brZeroAnd_brOr = new StepInfo("[BrZeroAnd_BrOr]: BR_ZERO_AND -> BR_OR", 
                                            ComponentID.BR_ZERO_AND, ComponentID.BR_OR, BusID.BrZeroAnd_BrOr, 
                                            brOr_in_brZeroAnd ? "1" : "0");
            microSteps.add(new MicroStep(Set.of(brZeroAnd_brOr),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 16:
            boolean MuxPCSrc_Signal = controlSignals.uncondBranch() || brOr_in_brFlagAnd || brOr_in_brZeroAnd;
            
            StepInfo brOr_MuxPCSrc = new StepInfo("[BrOr_MuxPCSrc_Signal]: BR_OR -> MUX_PCSrc", 
                                            ComponentID.BR_OR, ComponentID.MUX_PCSrc, BusID.BrOr_MuxPCSrc_Signal, 
                                            MuxPCSrc_Signal ? "1" : "0");
            microSteps.add(new MicroStep(Set.of(brOr_MuxPCSrc),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 17:
            brAdder_in_b = shiftLeft2_in << 2;
            
            StepInfo shiftLeft2_brAdder = new StepInfo("[ShiftLeft2_BranchAdder]: SHIFT_LEFT_2 -> BR_ADDER", 
                                            ComponentID.SHIFT_LEFT_2, ComponentID.BR_ADDER, BusID.ShiftLeft2_BranchAdder, 
                                            "0x" + Long.toHexString(brAdder_in_b) + " (" + brAdder_in_b + ")");
            microSteps.add(new MicroStep(Set.of(shiftLeft2_brAdder),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 18:
            MuxPCSrc_1 = brAdder_in_a + brAdder_in_b;
            
            StepInfo brAdder_MuxPCSrc = new StepInfo("[BranchAdder_MuxPCSrc_1]: BR_ADDER -> MUX_PCSrc", 
                                            ComponentID.BR_ADDER, ComponentID.MUX_PCSrc, BusID.BranchAdder_MuxPCSrc_1, 
                                            "0x" + Long.toHexString(MuxPCSrc_1));
            microSteps.add(new MicroStep(Set.of(brAdder_MuxPCSrc),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 19:
            StepInfo adder4_const = new StepInfo("[PCAdder4_const]: PC_ADDER4 -> MUX_PCSrc", 
                                            ComponentID.PC_ADDER4, ComponentID.MUX_PCSrc, BusID.PCAdder4_const, 
                                            "0x" + Long.toHexString(4));
            microSteps.add(new MicroStep(Set.of(adder4_const),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 20:
            MuxPCSrc_0 = adder4_in + 4;
            
            StepInfo adder4_MuxPCSrc = new StepInfo("[PCAdder4_MuxPCSrc_0]: PC_ADDER4 -> MUX_PCSrc", 
                                            ComponentID.PC_ADDER4, ComponentID.MUX_PCSrc, BusID.PCAdder4_MuxPCSrc_0, 
                                            "0x" + Long.toHexString(MuxPCSrc_0));
            microSteps.add(new MicroStep(Set.of(adder4_MuxPCSrc),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            // Micro-step 21:
            nextPC = MuxPCSrc_Signal ? MuxPCSrc_1 : MuxPCSrc_0;
            
            StepInfo MuxPCSrc_out = new StepInfo("[MuxPCSrc]: MUX_PCSrc -> PROGRAM_COUNTER", 
                                            ComponentID.MUX_PCSrc, ComponentID.PROGRAM_COUNTER, BusID.MuxPCSrc_ProgramCounter, 
                                            "0x" + Long.toHexString(nextPC));
            microSteps.add(new MicroStep(Set.of(MuxPCSrc_out),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));
            programCounter.setAddress(nextPC); 

            // Halt condition:
            // If the nextPC is the same as the currentPC and the instruction is not a branch, halt the simulation.
            if (MuxPCSrc_Signal && nextPC == currentPC && !mnemonic.equals("BR")) {
                halted = true;
                microSteps.add(new MicroStep(Set.of(new StepInfo("[Halted]: No more instructions", null, null, null, "Halted")),
                                            memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));
            }
        } catch (InvalidPCException | InvalidInstructionException | MemoryAccessException | IllegalArgumentException e) { 
            halted = true;
            String errorMsg = String.format("HALTED - Error: %s at PC=0x%X", e.getClass().getSimpleName(), currentPC); 
            System.err.println(ColoredLog.ERROR + errorMsg + ": " + e.getMessage());
            microSteps.add(new MicroStep(Set.of(new StepInfo(errorMsg, null, null, null, "Halted")),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));

            throw new SimulationException(e.getMessage(), e, currentPC, cycleCount);
        } catch (Exception e) {
            halted = true; 
            String errorMsg = String.format("HALTED - Unexpected Error at PC=0x%X", currentPC); 
            System.err.println(ColoredLog.ERROR + errorMsg + ": " + e.getMessage()); 
            e.printStackTrace();

            microSteps.add(new MicroStep(Set.of(new StepInfo(errorMsg, null, null, null, "Halted")),
                                        memoryController.getStorage(), registerController.getStorage(), programCounter.getCurrentAddress()));
            throw new SimulationException("Unexpected runtime error: " + e.getMessage(), e, currentPC, cycleCount);
        } finally {
            cycleCount++;
            // displayState();
        }
        return microSteps;
    }


    // --- Getters & Setters ---
    
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
    public boolean isHalted() {
        return halted;
    }


    // --- Debugging & Helpers ---
    /**
     * Displays the current internal state of the simulator (PC, registers, flags, cycle count)
     * to the standard output console. Primarily used for debugging.
     */
    public void displayState() {
        System.out.println("\n--- Simulator State ---");
        System.out.printf("Cycle Count: %d\n", cycleCount);
        System.out.printf("PC: 0x%X\n", programCounter.getCurrentAddress());
        System.out.println("Registers: " + registerController.getStorage().toString());
        System.out.println("Flags: N=" + flagN + ", Z=" + flagZ + ", C=" + flagC + ", V=" + flagV);
        System.out.println("Data Memory: " + memoryController.getStorage().toString());
        System.out.println("------------------------");
    }


}
