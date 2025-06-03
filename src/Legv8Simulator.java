// import java.util.*; 

// import legv8.assembler.*;
// import legv8.core.*;
// import legv8.exceptions.*;
// import legv8.instructions.*;
// import legv8.storage.*;
// import legv8.util.*;
// import legv8.datapath.*;
// import legv8.simulator.*;

// public class Legv8Simulator {
//     private final ProgramCounter programCounter;

//     Legv8Simulator() {
//         this.programCounter = new ProgramCounter();
//     }

//     Legv8Simulator(ProgramCounter programCounter) {
//         this.programCounter = programCounter;
//     }

//     public void assemble(List<String> assemblyLines) {
//         Assembler assembler = new Assembler();
//         List<Instruction> instructions = assembler.assemble(assemblyLines);
        
        
//         for (Instruction instruction : instructions) {
//             System.out.println(instruction);
//         }
//     }

//     public static void main(String[] args) {
//         InstructionConfigLoader configLoader = new InstructionConfigLoader();
//         if (!configLoader.loadConfig(".//resources/config/instructions_config.csv")) { System.err.println("FATAL: Config load failed."); return; }
//         InstructionFactory.initialize(configLoader);

        
//         ProgramCounter pc = new ProgramCounter();

        
//         List<String> testProgram = List.of( 
//             "/t/ Simple Test Program",
//             "ADDI X1, XZR, #10",    
//             "ADDI X2, XZR, #20",
//             "ADD X3, X1, X2",
//             "SUBIS XZR, X3, #30",   
//             "B.EQ is_equal",      
//             "ADDI X4, XZR, #1",
//             "is_equal:",            
//             "STUR X3, [SP, #0]",    
//             "LDUR X5, [SP, #0]",
//             "ADDI X9, XZR, #99",
//             "halt_loop:",           
//             "B halt_loop"
//         );
            
            
//         Assembler assembler = new Assembler();
//         InstructionMemory imem = new InstructionMemory();
//         try {
//             List<Instruction> instructions = assembler.assemble(testProgram);
//             imem.loadInstructions(instructions);
//             imem.displayMemorySummary();
//         } catch (AssemblyException e) {
//             System.err.println("Assembly failed: " + e.getMessage());
//             for (String error : assembler.getErrors()) {
//                 System.err.println("Error: " + error);
//             }
//         }

        
//         System.out.println("--- Testing MemoryStorage ---");
//         MemoryStorage memory = new MemoryStorage();
//         memory.writeLong(0x600000, 0x12345678);
//         memory.writeLong(0x600001, 0x12345678);
//         memory.displayMemoryContents(0x5FFFF0, 0x600010);

        
//         System.out.println("--- Testing RegisterFile ---");
//         RegisterStorage regStorage = new RegisterStorage();
//         RegisterFileController registerFile = new RegisterFileController(regStorage);
//         registerFile.writeRegister(0, 0x12345678, false);
//         registerFile.writeRegister(1, 0x87654321, true);
//         registerFile.writeRegister(31, 0x87654321, true);
//         registerFile.displayStorage();

        
//         System.out.println("--- Testing DataMemory ---");
//         DataMemoryController dataMemory = new DataMemoryController(memory);
//         dataMemory.accessMemory(0x600000, 0x12345678, true, false); 
//         dataMemory.accessMemory(0x600001, 0x87654321, true, false); 
//         System.out.println("Data at 0x600000: " + Long.toHexString(dataMemory.accessMemory(0x600000, 0, false, true))); 
//         System.out.println("Data at 0x600008: " + Long.toHexString(dataMemory.accessMemory(0x600008, 0, false, true))); 
//         memory.displayMemoryContents(0x600000, 0x600010); 

        
//         System.out.println("--- Testing ControlUnit ---");
//         ControlUnit controlUnit = new ControlUnit(configLoader);
//         for (Instruction instruction : imem.getInstructions()) {
            
//             controlUnit.decode(instruction.getBytecode());
//         }

        
//         System.out.println("--- Testing Engine ---");
//         SimulatorEngine engine = new SimulatorEngine(configLoader, imem);
//         System.err.println("Instruction Count: " + imem.getInstructionCount()); 
//         for (int i = 0; i < imem.getInstructionCount(); i++) {
//             try {
//                 System.out.println("------- Instruction: " + engine.getCurrentInstruction()+ " -------");
//                 System.err.println("------- Instruction: " + engine.getCurrentInstruction()+ " -------");
//                 int stepCount = 0;
//                 List<MicroStep> microSteps = engine.stepAndGetMicroSteps();
//                 for (MicroStep step : microSteps) {
//                     System.out.println("MicroStep " + stepCount + ": " + step);
//                     stepCount++;
//                 }
                
//             } catch (SimulationException e) {
//                 System.err.println("Simulation error: " + e.getMessage());
//             }
//         }

//         engine.displayState();
//     }
// }