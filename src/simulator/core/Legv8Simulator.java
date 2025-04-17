package simulator.core;

import simulator.instructions.*;
import simulator.storage.RegisterStorage; // Để lấy index XZR
import simulator.storage.MemoryStorage; 
import simulator.util.*; // ALUOperation, ALUResult, ControlSignals
import simulator.exceptions.*; // InvalidPCException, etc.
import simulator.assembler.Assembler; // Để thử nghiệm với chương trình mẫu

import simulator.gui.datapath.BusID; // Import enums mới
import simulator.gui.datapath.ComponentID;
import simulator.gui.datapath.MicroStepInfo;
import java.util.*; // Cần cho List, Set, Map

/**
 * The main LEGv8 CPU simulator engine.
 * Coordinates the datapath components through the fetch-decode-execute-memory-writeback cycle.
 */
public class Legv8Simulator {

    // --- Datapath Components ---
    private final ProgramCounter pc;
    private final InstructionMemory instructionMemory;
    private final ControlUnit controlUnit;
    private final RegisterFileController registerController;
    private final ArithmeticLogicUnit alu;
    private final DataMemoryController memoryController;
    // Các thành phần phụ trợ (Muxes, Adders, Shifters) - Chúng ta có thể không cần mô hình hóa
    // chúng như các đối tượng riêng biệt ở mức này nếu logic của chúng đơn giản và được tích hợp
    // trực tiếp vào luồng dữ liệu của simulator (ví dụ: chọn input cho ALU, tính toán PC+4).
    // Tuy nhiên, nếu muốn mô phỏng từng cổng logic, chúng ta có thể thêm chúng sau.

    // --- Simulation State ---
    private long cycleCount = 0;
    private boolean halted = false;
    private boolean externalHaltRequest = false; // Cờ để dừng từ bên ngoài (GUI)

    // --- CPU State Flags ---
    private boolean flagN = false; // Negative
    private boolean flagZ = false; // Zero
    private boolean flagC = false; // Carry
    private boolean flagV = false; // Overflow

    /**
     * Constructs the simulator with all necessary datapath components.
     */
    public Legv8Simulator(ProgramCounter pc, InstructionMemory imem, ControlUnit cu,
                          RegisterFileController rfc, ArithmeticLogicUnit alu, DataMemoryController dmc) {
        // Validate that all essential components are provided
        if (pc == null || imem == null || cu == null || rfc == null || alu == null || dmc == null) {
            throw new IllegalArgumentException("All core simulator components must be non-null.");
        }
        this.pc = pc;
        this.instructionMemory = imem;
        this.controlUnit = cu;
        this.registerController = rfc;
        this.alu = alu;
        this.memoryController = dmc;

        System.out.println("LEGv8 CPU Simulator Engine Initialized.");
        resetState(); // Initialize flags and cycle count
    }

    /**
     * Executes one full CPU cycle and returns a list of micro-steps for visualization.
     *
     * @return A List of MicroStepInfo detailing the actions within this cycle.
     * @throws SimulationException If a fatal error occurs during the cycle.
     */
    public List<MicroStepInfo> stepAndGetMicroSteps() throws SimulationException {
        if (halted || externalHaltRequest) {
            halted = true;
            System.out.println("Simulator::step() - Already halted or external request.");
            return Collections.emptyList(); // Trả về list rỗng nếu đã halt
        }

        List<MicroStepInfo> microSteps = new ArrayList<>();
        long currentPC = pc.getCurrentAddress();
        long cycleStartCycleCount = cycleCount;
        System.out.printf("\n--- Cycle %d --- PC=0x%X ---\n", cycleCount, currentPC);

        // --- Local variables to store intermediate results for micro-steps ---
        Instruction currentInstruction = null; 
        BitSet instructionBits = null; 
        InstructionDefinition definition = null;
        ControlSignals signals = ControlSignals.NOP(); 
        ALUOperation aluOperation = ALUOperation.IDLE;
        long nextSequentialPC = 0; 
        long readData1 = 0, readData2 = 0;
        long aluInputA = 0, aluInputB = 0;
        boolean currentN = flagN, currentZ = flagZ, currentC = flagC, currentV = flagV; // Trạng thái cờ đầu chu kỳ
        ALUResult aluResult = new ALUResult(0, currentN, currentZ, currentC, currentV); // Default result với cờ cũ
        long memoryAddress = 0; 
        long memoryWriteData = 0; 
        long memoryReadData = 0;
        long writeBackData = 0; 
        int writeRegAddr = -1; 
        boolean branchTaken = false; 
        long nextPC = 0;
        String mnemonic = "???"; char format = '?';

        try {
            // --- 1. FETCH ---
            // >> MicroStep 1: PC Output Ready <<
            microSteps.add(new MicroStepInfo("PC -> Addr Bus",
                Set.of(ComponentID.PC), Set.of(BusID.PC_OUT, BusID.PC_IMEM_ADDR), Map.of(BusID.PC_IMEM_ADDR, currentPC),
                signals, aluOperation, currentN, currentZ, currentC, currentV));

            // Action: Fetch from IMem
            currentInstruction = instructionMemory.fetch(currentPC); // Ném InvalidPCException nếu lỗi
            definition = currentInstruction.getDefinition();
            mnemonic = definition.getMnemonic();
            format = definition.getFormat();
            System.out.printf("  Fetched [%s]: %s\n", mnemonic, currentInstruction.disassemble()); // Log fetch
            instructionBits = currentInstruction.getBytecode();

            // >> MicroStep 2: Instruction Memory Output Ready <<
            microSteps.add(new MicroStepInfo("IMem -> Decode Units",
                Set.of(ComponentID.IMEM),
                Set.of(BusID.IMEM_OUT_INSTR, BusID.INSTR_CTRL_BITS, BusID.INSTR_REG1_ADDR, BusID.INSTR_REG2_ADDR, BusID.INSTR_WRITE_ADDR, BusID.INSTR_IMM_SEXT),
                Map.of(), signals, aluOperation, currentN, currentZ, currentC, currentV));

            // Action: Calculate PC+4 (làm song song với fetch)
            nextSequentialPC = currentPC + 4;
            
            // >> MicroStep 3: PC+4 Adder Output Ready <<
            microSteps.add(new MicroStepInfo("PC+4 Calculated",
                Set.of(ComponentID.PC_ADD_4), Set.of(BusID.PC4_OUT), Map.of(BusID.PC4_OUT, nextSequentialPC),
                signals, aluOperation, currentN, currentZ, currentC, currentV));


            // --- 2. DECODE ---
            // Action: Decode control signals
            ControlUnit.DecodeResult decodeResult = controlUnit.decode(instructionBits); // Ném InvalidInstructionException nếu lỗi
            signals = decodeResult.signals(); // Cập nhật signals
            aluOperation = decodeResult.aluOperation(); // Cập nhật aluOp

            // if (definition != decodeResult.definition()) definition = decodeResult.definition(); // Cập nhật nếu mismatch
            
            // >> MicroStep 4: Control Signals Generated <<
            // Các bus tín hiệu có thể được nhóm lại hoặc hiển thị riêng lẻ tùy layout GUI
             // >> MicroStep 4: Control Signals Generated <<
            Set<BusID> controlBuses = Set.of( BusID.CTRL_REGWRITE, BusID.CTRL_ALUSRC, BusID.CTRL_MEMWRITE, BusID.CTRL_MEMREAD, BusID.CTRL_MEMTOREG, BusID.CTRL_ZEROBRANCH, BusID.CTRL_FLAGBRANCH, BusID.CTRL_UNCONDBRANCH, BusID.CTRL_FLAGWRITE );
            microSteps.add(new MicroStepInfo("Control Signals Generated",
                Set.of(ComponentID.ID_CTRL), controlBuses, Map.of(),
                signals, aluOperation, currentN, currentZ, currentC, currentV)); // signals mới nhất


            // Action: Decode register addresses and immediate
            // ... (logic switch(format) để lấy readReg1Addr, readReg2Addr, writeRegAddr, immediateValue như cũ) ...
            int readReg1Addr = 31, readReg2Addr = 31; writeRegAddr = -1;
            long immediateValue=0; long signExtendedImm=0; boolean useAluOperandBOverride=false;
            switch (format) { /* ... logic switch case như cũ ... */
                case 'R': readReg1Addr = currentInstruction.getRn_R(); readReg2Addr = currentInstruction.getRm_R(); writeRegAddr = currentInstruction.getRd_R(); if (mnemonic.equals("BR")) { readReg2Addr = 31; writeRegAddr = -1; } break;
                case 'I': readReg1Addr = currentInstruction.getRn_I(); writeRegAddr = currentInstruction.getRd_I(); immediateValue = currentInstruction.getImmediate_I(); signExtendedImm = SignExtend.extend((int)immediateValue, 12); useAluOperandBOverride = true; break;
                case 'D': readReg1Addr = currentInstruction.getRn_D(); int rt_d = currentInstruction.getRt_D(); immediateValue = currentInstruction.getAddress_D(); signExtendedImm = SignExtend.extend((int)immediateValue, 9); useAluOperandBOverride = true; if (signals.memRead()) writeRegAddr = rt_d; else if (signals.memWrite()) { readReg2Addr = rt_d; writeRegAddr = -1; } break;
                case 'B': immediateValue = currentInstruction.getAddress_B(); signExtendedImm = SignExtend.extend((int)immediateValue, 26); if (mnemonic.equals("BL")) writeRegAddr = 30; break;
                case 'C': int rt_cb = currentInstruction.getRt_CB(); immediateValue = currentInstruction.getAddress_CB(); signExtendedImm = SignExtend.extend((int)immediateValue, 19); if (!mnemonic.startsWith("B.")) readReg1Addr = rt_cb; break;
                case 'M': writeRegAddr = currentInstruction.getRd_IM(); int imm16 = currentInstruction.getImmediate_IM(); int hw = currentInstruction.getShift_IM(); signExtendedImm = (long)(imm16 & 0xFFFF) << (hw * 16); useAluOperandBOverride = true; break;
            }
            long aluOperandBOverride = signExtendedImm; // Use the calculated sign-extended value

             // >> MicroStep 5: Sign Extend Output Ready (nếu có) <<
            if (format == 'I' || format == 'D' || format == 'B' || format == 'C' || format == 'M') {
                microSteps.add(new MicroStepInfo("Sign Extend Done",
                    Set.of(ComponentID.SIGN_EXTEND), Set.of(BusID.SEXT_OUT), Map.of(BusID.SEXT_OUT, signExtendedImm),
                    signals, aluOperation, currentN, currentZ, currentC, currentV));
            }

            // Action: Read Registers
            long[] regValues = registerController.readRegisters(readReg1Addr, readReg2Addr); // Ném IllegalArgument nếu lỗi reg num
            readData1 = regValues[0]; readData2 = regValues[1];

            // >> MicroStep 6: Register Read Data Ready <<
            microSteps.add(new MicroStepInfo("Register Read Done",
                Set.of(ComponentID.REG_FILE), Set.of(BusID.REG_READ_DATA1, BusID.REG_READ_DATA2),
                Map.of(BusID.REG_READ_DATA1, readData1, BusID.REG_READ_DATA2, readData2),
                signals, aluOperation, currentN, currentZ, currentC, currentV));


            // --- 3. EXECUTE ---
            // Action: Select ALU inputs
            aluInputA = readData1; 
            aluInputB = signals.aluSrc() ? aluOperandBOverride : readData2;

            // >> MicroStep 7: ALU Inputs Ready <<
            Set<ComponentID> aluInputComps = new HashSet<>(Set.of(ComponentID.EX_ALU, ComponentID.EX_MUX_ALUSRC));
            Set<BusID> aluInputBuses = new HashSet<>(Set.of(BusID.ALU_IN_A, BusID.ALU_IN_B_MUX_OUT));
            Map<BusID, Long> aluInputValues = new HashMap<>(Map.of(BusID.ALU_IN_A, aluInputA, BusID.ALU_IN_B_MUX_OUT, aluInputB));
            aluInputBuses.add(signals.aluSrc() ? BusID.SEXT_OUT : BusID.REG_READ_DATA2); 
            aluInputBuses.add(BusID.REG_READ_DATA1);
            microSteps.add(new MicroStepInfo("ALU Inputs Ready", aluInputComps, aluInputBuses, aluInputValues, signals, aluOperation, currentN, currentZ, currentC, currentV));

            // Action: Execute ALU operation (or skip)
            boolean shouldExecuteAlu = !(format == 'B' || mnemonic.startsWith("B."));
            if (shouldExecuteAlu) { aluResult = alu.execute(aluInputA, aluInputB, aluOperation); }
            // else aluResult giữ giá trị default với cờ cũ

             // >> MicroStep 8: ALU Result & Flags Ready <<
             String aluDesc = shouldExecuteAlu ? "ALU Exec Done: " + aluOperation : "ALU Skipped (" + mnemonic + ")";
             microSteps.add(new MicroStepInfo(aluDesc,
                 Set.of(ComponentID.EX_ALU, ComponentID.NZCV_FLAGS), Set.of(BusID.ALU_OUT_RESULT, BusID.NZCV_OUT),
                 Map.of(BusID.ALU_OUT_RESULT, aluResult.result()), signals, aluOperation,
                 aluResult.negativeFlag(), aluResult.zeroFlag(), aluResult.carryFlag(), aluResult.overflowFlag()));

            // Action & State Update: Cập nhật cờ CPU nếu cần
            if (shouldExecuteAlu && signals.flagWrite()) {
                this.flagN = aluResult.negativeFlag(); this.flagZ = aluResult.zeroFlag();
                this.flagC = aluResult.carryFlag(); this.flagV = aluResult.overflowFlag();
                currentN = flagN; currentZ = flagZ; currentC = flagC; currentV = flagV;
                 // Không cần micro-step riêng, cờ đã hiển thị ở step 8
                 // System.out.printf("  Flags Updated: N=%b Z=%b C=%b V=%b\n", flagN, flagZ, flagC, flagV);
            }

            // --- 4. MEMORY ---
            memoryAddress = aluResult.result(); memoryWriteData = readData2;

            if (signals.memRead() || signals.memWrite()) {
                 // >> MicroStep 9: Memory Access Setup <<
                 Set<BusID> memSetupBuses = new HashSet<>(Set.of(BusID.MEM_ADDRESS, BusID.ALU_OUT_RESULT));
                 Map<BusID, Long> memSetupValues = new HashMap<>(Map.of(BusID.MEM_ADDRESS, memoryAddress));
                 if (signals.memWrite()) { memSetupBuses.add(BusID.MEM_WRITE_DATA); memSetupValues.put(BusID.MEM_WRITE_DATA, memoryWriteData); memSetupBuses.add(BusID.REG_READ_DATA2); }
                 microSteps.add(new MicroStepInfo("Memory Access Setup", Set.of(ComponentID.MEM_DATA), memSetupBuses, memSetupValues, signals, aluOperation, currentN, currentZ, currentC, currentV));

                // Action: Access Memory
                memoryReadData = memoryController.accessMemory(memoryAddress, memoryWriteData, signals.memWrite(), signals.memRead()); // Ném MemoryAccessException nếu lỗi

                // >> MicroStep 10: Memory Read Result Ready (nếu MemRead) <<
                if (signals.memRead()) {
                    microSteps.add(new MicroStepInfo("Memory Read Done", Set.of(ComponentID.MEM_DATA), Set.of(BusID.MEM_READ_DATA), Map.of(BusID.MEM_READ_DATA, memoryReadData), signals, aluOperation, currentN, currentZ, currentC, currentV));
                }
            }

            // --- 5. WRITE BACK ---
            // Action: Select Write Back Data
             boolean isLoad = signals.memRead() && signals.memToReg(); boolean isAluToReg = signals.regWrite() && !signals.memToReg() && writeRegAddr!=-1 && writeRegAddr!=31; boolean isLink = mnemonic.equals("BL");
             if (isLink) { writeBackData = nextSequentialPC; }
             else if (isLoad) { writeBackData = memoryReadData;}
             else { writeBackData = aluResult.result();}
             if (mnemonic.equals("MOVK") && signals.regWrite() && writeRegAddr != 31) { /* ... logic merge MOVK ... */ long curr=registerController.readSingleRegister(writeRegAddr);int imm16=currentInstruction.getImmediate_IM();int hw=currentInstruction.getShift_IM();long sh=(long)hw*16;long mask=0xFFFFL<<sh;long shiftedImm=(long)(imm16&0xFFFF)<<sh;writeBackData=(curr&~mask)|shiftedImm;}

            // >> MicroStep 11: Write Back Data Ready <<
             BusID wbDataSourceBus = BusID.ALU_OUT_RESULT; if(isLoad) wbDataSourceBus = BusID.MEM_READ_DATA; if(isLink) wbDataSourceBus = BusID.PC4_OUT;
             microSteps.add(new MicroStepInfo("Write Back Data Select", Set.of(ComponentID.WB_MUX_MEMWB), Set.of(wbDataSourceBus, BusID.WB_DATA_MUX_OUT), Map.of(BusID.WB_DATA_MUX_OUT, writeBackData), signals, aluOperation, currentN, currentZ, currentC, currentV));

            // Action: Write to Register File (if needed)
            boolean actualWrite = signals.regWrite() && writeRegAddr != -1 && writeRegAddr != RegisterStorage.ZERO_REGISTER_INDEX;
            if (actualWrite) {
                 // >> MicroStep 12: Register Write Complete <<
                  microSteps.add(new MicroStepInfo("Register Write to X" + writeRegAddr, Set.of(ComponentID.REG_FILE), Set.of(BusID.WB_DATA_MUX_OUT, BusID.WB_WRITE_REG_DATA, BusID.WB_WRITE_REG_ADDR), Map.of(BusID.WB_WRITE_REG_DATA, writeBackData), signals, aluOperation, currentN, currentZ, currentC, currentV));
                registerController.writeRegister(writeRegAddr, writeBackData, true); // Ném IllegalArgument nếu lỗi reg num
            }

            // --- 6. PC Update ---
            nextPC = nextSequentialPC; branchTaken = false; long branchTargetPC = 0;

            // Action: Calculate branch target if needed
            if (format == 'B' || format == 'C') {
                 long branchOffsetRaw = (format == 'B') ? SignExtend.extend((int)immediateValue, 26) : SignExtend.extend((int)immediateValue, 19);
                 branchTargetPC = currentPC + (branchOffsetRaw * 4);
                 // >> MicroStep 13: Branch Target Ready <<
                  microSteps.add(new MicroStepInfo("Branch Target Calculated", Set.of(ComponentID.SIGN_EXTEND, ComponentID.BR_SHIFT_LEFT, ComponentID.BR_ADD_TARGET), Set.of(BusID.BRANCH_TARGET_ADDR), Map.of(BusID.BRANCH_TARGET_ADDR, branchTargetPC), signals, aluOperation, currentN, currentZ, currentC, currentV));
            }

            // Action: Determine if branch is taken
            boolean conditionMet = false;
            if (signals.uncondBranch()) { conditionMet = true; if(mnemonic.equals("BR")) branchTargetPC = readData1; }
            else if (signals.flagBranch()) { conditionMet = checkCondition(currentInstruction.getRt_CB()); }
            else if (signals.zeroBranch()) { conditionMet = (mnemonic.equals("CBZ") && flagZ) || (mnemonic.equals("CBNZ") && !flagZ); }
            branchTaken = conditionMet;

             // >> MicroStep 14: Next PC Selected <<
            if (branchTaken) { nextPC = branchTargetPC; }
             BusID pcMuxInput0 = BusID.PC4_OUT; BusID pcMuxInput1 = mnemonic.equals("BR") ? BusID.REG_READ_DATA1 : BusID.BRANCH_TARGET_ADDR;
             microSteps.add(new MicroStepInfo("Next PC Selected (Branch=" + branchTaken + ")", Set.of(ComponentID.BR_MUX_PCSrc), Set.of(branchTaken ? pcMuxInput1 : pcMuxInput0, BusID.PC_SRC_MUX_OUT), Map.of(BusID.PC_SRC_MUX_OUT, nextPC), signals, aluOperation, currentN, currentZ, currentC, currentV));

            // >> MicroStep 15: PC Update <<
             microSteps.add(new MicroStepInfo("PC Update", Set.of(ComponentID.PC), Set.of(BusID.PC_SRC_MUX_OUT, BusID.PC_IN), Map.of(BusID.PC_IN, nextPC), signals, aluOperation, currentN, currentZ, currentC, currentV));

            // Action: Set PC for the *next* cycle
            pc.setAddress(nextPC);

            // Action: Check for halt condition
            if (branchTaken && nextPC == currentPC && !mnemonic.equals("BR")) {
                halted = true;
                 microSteps.add(new MicroStepInfo("HALTED - Branch to Self", Set.of(), Set.of(), Map.of(), signals, aluOperation, currentN, currentZ, currentC, currentV));
            }

        // --- Error Handling ---
        } catch (InvalidPCException | InvalidInstructionException | MemoryAccessException | IllegalArgumentException e) { // Thêm IllegalArgumentException
            halted = true; String errorMsg = String.format("HALTED - Error: %s at PC=0x%X", e.getClass().getSimpleName(), currentPC); System.err.println(errorMsg + ": " + e.getMessage());
            microSteps.add(new MicroStepInfo(errorMsg, Set.of(), Set.of(), Map.of(), signals, aluOperation, currentN, currentZ, currentC, currentV));
            throw new SimulationException(e.getMessage(), e, currentPC, cycleStartCycleCount);
        } catch (Exception e) {
             halted = true; String errorMsg = String.format("HALTED - Unexpected Error at PC=0x%X", currentPC); System.err.println(errorMsg + ": " + e.getMessage()); e.printStackTrace();
             microSteps.add(new MicroStepInfo(errorMsg, Set.of(), Set.of(), Map.of(), signals, aluOperation, currentN, currentZ, currentC, currentV));
             throw new SimulationException("Unexpected runtime error: " + e.getMessage(), e, currentPC, cycleStartCycleCount);
        } finally {
            cycleCount++;
        }
        return microSteps;
    }

    // --- HÀM step() ĐƠN GIẢN HÓA CHO run() ---
    /**
     * Executes a single cycle without generating detailed micro-steps.
     * Primarily used by the run() method for faster execution.
     * Does not throw SimulationException outwards, sets halted flag instead.
     * @return true if simulation can continue, false if halted or error occurred.
     */
    private boolean stepInternal() /* Bỏ throws SimulationException */ { // <--- Thay đổi 1
        if (halted || externalHaltRequest) {
           halted = true;
           return false;
        }

        long currentPC = pc.getCurrentAddress();
        long cycleStartCycleCount = cycleCount; // Dùng để báo lỗi nếu cần
        
        // --- Local variables for this cycle ---
        Instruction currentInstruction = null;
        InstructionDefinition definition = null;
        ControlSignals signals = ControlSignals.NOP();
        ALUOperation aluOperation = ALUOperation.IDLE;
        String mnemonic = "???";
        char format = '?';
        // System.out.printf("\n--- Cycle %d --- PC=0x%X ---\n", cycleCount, currentPC);

        try {
            // --- 1. FETCH ---
            currentInstruction = instructionMemory.fetch(currentPC); // Fetch instruction object
            definition = currentInstruction.getDefinition();         // Get definition from instruction
            mnemonic = definition.getMnemonic();                   // Get mnemonic from definition
            format = definition.getFormat();                       // Get format from definition
            System.out.printf("  Fetched [%s]: %s\n", mnemonic, currentInstruction.disassemble()); // Log fetched instruction
            BitSet instructionBits = currentInstruction.getBytecode();
            long nextSequentialPC = currentPC + 4;

            // --- 2. DECODE ---
            ControlUnit.DecodeResult decodeResult = controlUnit.decode(instructionBits);
            signals = decodeResult.signals();       // Get signals for this instruction
            aluOperation = decodeResult.aluOperation(); // Get ALU operation for this instruction

            // ControlUnit.DecodeResult decodeResult = controlUnit.decode(instructionBits);
            // ControlSignals signals = decodeResult.signals();
            // ALUOperation aluOperation = decodeResult.aluOperation();
            // char format = definition.getFormat(); String mnemonic = definition.getMnemonic();
            int readReg1Addr = 31, readReg2Addr = 31, writeRegAddr = -1;
            long immediateValue=0, signExtendedImm=0;
            boolean useAluOperandBOverride=false;

            switch (format) {
                case 'R': readReg1Addr = currentInstruction.getRn_R(); readReg2Addr = currentInstruction.getRm_R(); writeRegAddr = currentInstruction.getRd_R(); if (mnemonic.equals("BR")) { readReg2Addr = 31; writeRegAddr = -1; } break;
                case 'I': readReg1Addr = currentInstruction.getRn_I(); writeRegAddr = currentInstruction.getRd_I(); immediateValue = currentInstruction.getImmediate_I(); signExtendedImm = SignExtend.extend((int)immediateValue, 12); useAluOperandBOverride = true; break;
                case 'D': readReg1Addr = currentInstruction.getRn_D(); int rt_d = currentInstruction.getRt_D(); immediateValue = currentInstruction.getAddress_D(); signExtendedImm = SignExtend.extend((int)immediateValue, 9); useAluOperandBOverride = true; if (signals.memRead()) writeRegAddr = rt_d; else if (signals.memWrite()) { readReg2Addr = rt_d; writeRegAddr = -1; } break; // writeRegAddr=-1 for STUR moved here
                case 'B': immediateValue = currentInstruction.getAddress_B(); signExtendedImm = SignExtend.extend((int)immediateValue, 26); if (mnemonic.equals("BL")) writeRegAddr = 30; break;
                case 'C': int rt_cb = currentInstruction.getRt_CB(); immediateValue = currentInstruction.getAddress_CB(); signExtendedImm = SignExtend.extend((int)immediateValue, 19); if (!mnemonic.startsWith("B.")) readReg1Addr = rt_cb; break;
                case 'M': writeRegAddr = currentInstruction.getRd_IM(); int imm16 = currentInstruction.getImmediate_IM(); int hw = currentInstruction.getShift_IM(); signExtendedImm = (long)(imm16 & 0xFFFF) << (hw * 16); useAluOperandBOverride = true; break;
            }
            long aluOperandBOverride = signExtendedImm; // Use the calculated sign-extended value

            // Read registers
            long[] regValues = registerController.readRegisters(readReg1Addr, readReg2Addr);
            long readData1 = regValues[0]; long readData2 = regValues[1];

            // --- 3. EXECUTE ---
            ALUResult aluResult; 
            boolean shouldExecuteAlu = !(format == 'B' || mnemonic.startsWith("B.")); // Skip ALU for B, BL, B.cond

            if (shouldExecuteAlu) {
                long aluInputA = readData1;
                long aluInputB = signals.aluSrc() ? aluOperandBOverride : readData2;
                aluResult = alu.execute(aluInputA, aluInputB, aluOperation);
                // System.out.printf("  ALU Exec (%s): ... -> %s\n", aluOperation, aluResult); // Log chi tiết nếu cần
            } else {
                 aluResult = new ALUResult(0, flagN, flagZ, flagC, flagV); // Giữ cờ cũ nếu skip
                 // System.out.printf("  ALU Exec SKIPPED for %s\n", mnemonic);
            }

            // Update Flags if needed
            if (shouldExecuteAlu && signals.flagWrite()) {
                this.flagN = aluResult.negativeFlag(); this.flagZ = aluResult.zeroFlag();
                this.flagC = aluResult.carryFlag(); this.flagV = aluResult.overflowFlag();
                 // System.out.printf("  Flags Updated: N=%b Z=%b C=%b V=%b\n", flagN, flagZ, flagC, flagV);
            }

            // --- 4. MEMORY ---
            long memoryAddress = aluResult.result();
            long memoryWriteData = readData2;
            long memoryReadData = 0;
            if (signals.memRead() || signals.memWrite()) {
                memoryReadData = memoryController.accessMemory(memoryAddress, memoryWriteData, signals.memWrite(), signals.memRead());
            }

            // --- 5. WRITE BACK ---
            long writeBackData;
             if (mnemonic.equals("BL")) { writeBackData = nextSequentialPC; }
             else if (mnemonic.equals("MOVK") && signals.regWrite() && writeRegAddr != RegisterStorage.ZERO_REGISTER_INDEX) {
                 long currentValue = registerController.readSingleRegister(writeRegAddr);
                 int imm16 = currentInstruction.getImmediate_IM(); int hw = currentInstruction.getShift_IM();
                 long shiftAmount = hw * 16; long mask = 0xFFFFL << shiftAmount; long shiftedImmediate = (long)(imm16 & 0xFFFF) << shiftAmount;
                 writeBackData = (currentValue & ~mask) | shiftedImmediate;
             } else { writeBackData = signals.memToReg() ? memoryReadData : aluResult.result(); }

            if (writeRegAddr != -1) {
                registerController.writeRegister(writeRegAddr, writeBackData, signals.regWrite());
            }

            // --- 6. PC Update ---
            long nextPC = nextSequentialPC;
            boolean branchTaken = false;
            long branchTargetPC = 0;
            if (format == 'B' || format == 'C') {
                long branchOffsetRaw = (format == 'B') ? SignExtend.extend((int)immediateValue, 26) : SignExtend.extend((int)immediateValue, 19);
                branchTargetPC = currentPC + (branchOffsetRaw * 4);
            }
            boolean conditionMet = false;
            if (signals.uncondBranch()) { conditionMet = true; if(mnemonic.equals("BR")) branchTargetPC = readData1; }
            else if (signals.flagBranch()) { conditionMet = checkCondition(currentInstruction.getRt_CB()); }
            else if (signals.zeroBranch()) { conditionMet = (mnemonic.equals("CBZ") && flagZ) || (mnemonic.equals("CBNZ") && !flagZ); }
            branchTaken = conditionMet;
            if (branchTaken) { nextPC = branchTargetPC; }

            pc.setAddress(nextPC);

            // Check halt
            if (branchTaken && nextPC == currentPC && !mnemonic.equals("BR")) {
                halted = true;
                System.out.println("--- Simulation Halted (Branch to self) ---");
            }

            // --- Error Handling ---
            // Chỉ bắt các exception cụ thể, không bắt SimulationException vì hàm này không ném nó
        } catch (InvalidPCException | InvalidInstructionException | MemoryAccessException e) {
            halted = true; System.err.printf("*** Simulation HALTED on Cycle %d at PC=0x%X: %s ***\n", cycleStartCycleCount, currentPC, e.getMessage());
            cycleCount++; return false; // Dừng và trả về false
        } catch (Exception e) {
            halted = true; System.err.printf("*** Simulation HALTED unexpectedly on Cycle %d at PC=0x%X ***\n", cycleStartCycleCount, currentPC); e.printStackTrace();
            cycleCount++; return false; // Dừng và trả về false
        }

        cycleCount++; // Tăng cycle nếu không có lỗi
        return !halted;
    }

    /** Runs the simulation until halted or max cycles reached, without generating micro-steps. */
    public void run(int maxCycles) {
        System.out.println("\n--- Starting Simulation Run (Max Cycles: " + maxCycles + ") ---");

        while (cycleCount < maxCycles && !halted && !externalHaltRequest) {
            // Không cần try-catch ở đây nữa vì stepInternal đã xử lý lỗi và trả về false
            if (!stepInternal()) { // Gọi hàm step đơn giản hóa
                 break; // Exit loop if stepInternal() returns false (halted or error)
            }
        }
        System.out.printf("\n--- Simulation Run Finished --- (Cycles: %d, Halted: %b, External Halt: %b)\n",
                          cycleCount, halted, externalHaltRequest);
        printFinalState(); // In trạng thái cuối cùng
    }

    /** Requests the simulation to halt gracefully at the end of the current cycle. */
    public void requestHalt() {
        this.externalHaltRequest = true;
        System.out.println("Simulator: External halt requested.");
    }

    /** Resets the simulator state (PC, registers, memory, flags, counters). */
    public void reset() {
        System.out.println("\n--- Resetting Simulator ---");
        pc.reset();
        registerController.clearStorage();
        memoryController.clearStorage();
        resetState();
        System.out.println("Simulator Reset Complete.");
    }

    /** Resets internal state variables (flags, counters). */
    private void resetState() {
        cycleCount = 0;
        halted = false;
        externalHaltRequest = false;
        flagN = false;
        flagZ = false;
        flagC = false;
        flagV = false;
    }

    /** Prints the final state of the simulator components. */
    public void printFinalState() {
        System.out.println("\n--- Final Simulator State ---");
        System.out.println(pc);
        System.out.println(registerController.getStorage()); // Print register values
        System.out.println(memoryController.getStorage());   // Print memory values
        System.out.printf("Flags: N=%b Z=%b C=%b V=%b\n", flagN, flagZ, flagC, flagV);
        System.out.println("Cycles Executed: " + cycleCount);
        System.out.println("Halted: " + halted);
        System.out.println("-----------------------------");
    }

    // --- Getters for components (useful for GUI) ---
    public ProgramCounter getPc() { return pc; }
    public InstructionMemory getInstructionMemory() { return instructionMemory; }
    public RegisterFileController getRegisterController() { return registerController; }
    public DataMemoryController getMemoryController() { return memoryController; }
    public long getCycleCount() { return cycleCount; }
    public boolean isHalted() { return halted; }
    public boolean getFlagN() { return flagN; }
    public boolean getFlagZ() { return flagZ; }
    public boolean getFlagC() { return flagC; }
    public boolean getFlagV() { return flagV; }

    // --- Private Helper Methods ---

    /** Checks if the processor condition flags match the B.cond code. */
    private boolean checkCondition(int condCode) {
        // Mask to ensure only 4 bits are considered
        condCode &= 0xF;
        return switch (condCode) {
            case 0b0000 -> flagZ;              // EQ: Z == 1
            case 0b0001 -> !flagZ;             // NE: Z == 0
            case 0b0010 -> flagC;              // CS/HS: C == 1
            case 0b0011 -> !flagC;             // CC/LO: C == 0
            case 0b0100 -> flagN;              // MI: N == 1
            case 0b0101 -> !flagN;             // PL: N == 0
            case 0b0110 -> flagV;              // VS: V == 1
            case 0b0111 -> !flagV;             // VC: V == 0
            case 0b1000 -> flagC && !flagZ;   // HI: C == 1 and Z == 0
            case 0b1001 -> !(flagC && !flagZ); // LS: C == 0 or Z == 1
            case 0b1010 -> flagN == flagV;     // GE: N == V
            case 0b1011 -> flagN != flagV;     // LT: N != V
            case 0b1100 -> !flagZ && (flagN == flagV); // GT: Z == 0 and N == V
            case 0b1101 -> flagZ || (flagN != flagV); // LE: Z == 1 or N != V
            // 1110 (AL - Always) and 1111 (NV - Never) are typically not used for B.cond
            default -> {
                System.err.println("Warning: checkCondition called with invalid code: " + condCode);
                yield false; // Or throw exception
            }
        };
    }

     /** Gets the mnemonic suffix for B.cond (for logging). */
    private String getConditionMnemonic(int condCode) {
        condCode &= 0xF;
        return switch (condCode) {
             case 0b0000 -> "EQ"; case 0b0001 -> "NE"; case 0b0010 -> "CS"; case 0b0011 -> "CC";
             case 0b0100 -> "MI"; case 0b0101 -> "PL"; case 0b0110 -> "VS"; case 0b0111 -> "VC";
             case 0b1000 -> "HI"; case 0b1001 -> "LS"; case 0b1010 -> "GE"; case 0b1011 -> "LT";
             case 0b1100 -> "GT"; case 0b1101 -> "LE"; default -> "??";
        };
    }

    // --- Main Method for Testing ---
    public static void main(String[] args) {
        // --- Initialization ---
        InstructionConfigLoader loader = new InstructionConfigLoader();
        if (!loader.loadConfig("/instructions_config.csv")) { System.err.println("FATAL: Config load failed."); return; }
        InstructionFactory.initialize(loader);
        ProgramCounter pc = new ProgramCounter();
        InstructionMemory imem = new InstructionMemory();
        ControlUnit cu = new ControlUnit(loader);
        RegisterStorage rstore = new RegisterStorage();
        RegisterFileController rfc = new RegisterFileController(rstore);
        ArithmeticLogicUnit alu = new ArithmeticLogicUnit();
        MemoryStorage dstore = new MemoryStorage();
        DataMemoryController dmc = new DataMemoryController(dstore);
        Legv8Simulator simulator = new Legv8Simulator(pc, imem, cu, rfc, alu, dmc);

        // --- Load Program ---
        List<String> testProgram = List.of( /* ... code assembly như cũ ... */
            "// Simple Test Program",
            "ADDI X1, XZR, #10",    "ADDI X2, XZR, #20",    "ADD X3, X1, X2",
            "SUBIS XZR, X3, #30",   "B.EQ is_equal",      "ADDI X4, XZR, #1",
            "is_equal:",            "STUR X3, [SP, #0]",    "LDUR X5, [SP, #0]",
            "ADDI X9, XZR, #99",    "halt_loop:",           "B halt_loop"
        );
        Assembler assembler = new Assembler();
        try {
            List<Instruction> assembledInstructions = assembler.assemble(testProgram);
            imem.loadInstructions(assembledInstructions);
            // imem.displayMemorySummary();

            // --- Run Simulation using MicroSteps ---
            System.out.println("\n--- Running Step-by-Step (MicroSteps Grouped) ---");
            int maxCycles = 15;
            while (!simulator.isHalted() && simulator.getCycleCount() < maxCycles) {
                try {
                    List<MicroStepInfo> steps = simulator.stepAndGetMicroSteps(); // Lấy danh sách micro-steps
                    if (simulator.isHalted() && steps.isEmpty()) { // Kiểm tra nếu halt ngay từ đầu
                        break;
                    }
                    System.out.printf("--- Micro-steps for Cycle %d ---\n", simulator.getCycleCount() - 1);
                    int microIdx = 0;
                    for (MicroStepInfo step : steps) {
                        // In thông tin micro-step chi tiết hơn để debug
                         System.out.printf("  MS %2d: %-30s | Comps: %-35s | Buses: %s\n", // Tăng độ rộng
                                          microIdx++,
                                          step.description(),
                                          step.activeComponents(),
                                          step.activeBuses()
                                          /* step.busValues() // Có thể quá dài */ );
                         // In thêm giá trị bus nếu cần debug
                         // if (!step.busValues().isEmpty()) {
                         //     System.out.println("         Values: " + step.busValues());
                         // }
                    }
                    if (simulator.isHalted()) { // Kiểm tra halt sau khi xử lý microsteps
                        System.out.println("--- Simulation Halted ---");
                        break;
                    }
                } catch (SimulationException e) { // Bắt lỗi từ stepAndGetMicroSteps
                    System.err.println(e.getMessage()); // Lỗi đã được log bên trong, chỉ cần in message
                    break; // Dừng vòng lặp
                }
            }
            simulator.printFinalState();

        } catch (AssemblyException e) { System.err.println("\nAssembly Failed:\n" + e.getMessage());
        } catch (Exception e) { System.err.println("\nRuntime Error:\n" + e); e.printStackTrace(); }
    }
}