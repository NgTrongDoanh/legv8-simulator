import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import legv8.assembler.Assembler;
import legv8.core.ControlUnit;
import legv8.core.InstructionMemory;
import legv8.instructions.Instruction;
import legv8.instructions.InstructionConfigLoader;
import legv8.instructions.InstructionFactory;
import legv8.simulator.SimulatorEngine;

public final class RegressionTests {
    private static final long BASE = 0x400000L;
    private static int assertions;

    private RegressionTests() {}

    private static void assertEquals(String name, long expected, long actual) {
        assertions++;
        if (expected != actual) {
            throw new AssertionError(name + ": expected 0x" + Long.toHexString(expected)
                + ", got 0x" + Long.toHexString(actual));
        }
    }

    private static void assertEquals(String name, String expected, String actual) {
        assertions++;
        if (!expected.equals(actual)) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(String name, boolean condition) {
        assertions++;
        if (!condition) throw new AssertionError(name);
    }

    private static SimulatorEngine engine(InstructionConfigLoader loader, String... source) {
        List<Instruction> instructions = new Assembler().assemble(List.of(source));
        InstructionMemory memory = new InstructionMemory();
        memory.loadInstructions(instructions);
        return new SimulatorEngine(loader, memory);
    }

    private static void execute(SimulatorEngine engine, int instructionCount) throws Exception {
        for (int i = 0; i < instructionCount; i++) engine.step();
    }

    private static void testAssemblerAndDecoder(InstructionConfigLoader loader) throws Exception {
        assertTrue("BL is loaded from quoted CSV", loader.getDefinitionByMnemonic("BL") != null);

        Path invalidConfig = Files.createTempFile("legv8-invalid-config-", ".csv");
        try {
            Files.writeString(invalidConfig,
                "Mnemonic,Format,OpcodeID(Bin),Reg2Loc,UncondBranch,FlagBranch,ZeroBranch,MemRead,MemToReg,MemWrite,FlagWrite,ALUSrc,ALUOp,RegWrite,ALUControlOut,Note\n"
                + "BROKEN,R,not-binary,0,0,0,0,0,0,0,0,0,10,1,0010,test\n");
            int definitionCount = loader.getMnemonicMap().size();
            assertTrue("partial configuration is rejected", !loader.loadConfig(invalidConfig.toString()));
            assertEquals("rejected config retains prior definitions", definitionCount, loader.getMnemonicMap().size());
            assertTrue("rejected config retains BL", loader.getDefinitionByMnemonic("BL") != null);
        } finally {
            Files.deleteIfExists(invalidConfig);
        }

        assertTrue("generic B.cond lookup is fail-closed", loader.getDefinition(0b01010100, 'C') == null);

        List<Instruction> inlineLabel = new Assembler().assemble(List.of(
            "loop: ADDI X1, XZR, #4095",
            "B loop"
        ));
        assertEquals("inline label instruction count", 2, inlineLabel.size());
        assertEquals("unsigned I immediate", "ADDI   X1, X31, #4095", inlineLabel.get(0).disassemble());

        List<Instruction> signedOffset = new Assembler().assemble(List.of("LDUR X1, [X2, #-8]"));
        assertEquals("signed D offset", "LDUR   X1, [X2, #-8]", signedOffset.get(0).disassemble());

        List<Instruction> logicalInstructions = new Assembler().assemble(List.of(
            "ANDS X1, X2, X3",
            "EOR X4, X5, X6"
        ));
        assertEquals("ANDS opcode remains distinct", "ANDS", logicalInstructions.get(0).getDefinition().getMnemonic());
        assertEquals("EOR opcode remains distinct", "EOR", logicalInstructions.get(1).getDefinition().getMnemonic());

        Instruction bne = new Assembler().assemble(List.of("B.NE #1")).get(0);
        ControlUnit.DecodeResult decoded = new ControlUnit(loader).decode(bne.getBytecode());
        assertEquals("B.cond decode", "B.NE", decoded.definition().getMnemonic());

        for (String sample : List.of("arithmetic.s", "shift.s", "i-format.s", "memory_access.s", "branch.s", "all.s")) {
            List<String> lines = Files.readAllLines(Path.of("resources/src_test/norm", sample));
            assertTrue("sample assembles: " + sample, !new Assembler().assemble(lines).isEmpty());
        }
    }

    private static void testConditionalBranches(InstructionConfigLoader loader) throws Exception {
        SimulatorEngine bne = engine(loader,
            "MOVZ X1, #1",
            "ADDS X2, X1, XZR",
            "B.NE target",
            "MOVZ X3, #0xBAD",
            "target: MOVZ X4, #7"
        );
        execute(bne, 3);
        assertEquals("B.NE target", BASE + 16, bne.getProgramCounter().getCurrentAddress());

        SimulatorEngine bmi = engine(loader,
            "MOVZ X1, #1",
            "SUBS X2, XZR, X1",
            "B.MI target",
            "MOVZ X3, #0xBAD",
            "target: MOVZ X4, #7"
        );
        execute(bmi, 3);
        assertEquals("B.MI flag ordering", BASE + 16, bmi.getProgramCounter().getCurrentAddress());
    }

    private static void testLogicalOpcodes(InstructionConfigLoader loader) throws Exception {
        SimulatorEngine logical = engine(loader,
            "MOVZ X2, #0xF0",
            "MOVZ X3, #0x0F",
            "ANDS X1, X2, X3",
            "EOR X4, X2, X3"
        );
        execute(logical, 4);
        assertEquals("ANDS execution", 0, logical.getRegisterController().readRegister(1));
        assertEquals("EOR execution", 0xFF, logical.getRegisterController().readRegister(4));
    }

    private static void testBranchAndLink(InstructionConfigLoader loader) throws Exception {
        SimulatorEngine br = engine(loader,
            "MOVZ X5, #0x40, LSL #16",
            "ADDI X5, X5, #16",
            "BR X5",
            "MOVZ X1, #0xBAD",
            "MOVZ X2, #7"
        );
        execute(br, 3);
        assertEquals("BR register target", BASE + 16, br.getProgramCounter().getCurrentAddress());

        SimulatorEngine bl = engine(loader,
            "BL target",
            "MOVZ X1, #0xBAD",
            "target: MOVZ X2, #7"
        );
        execute(bl, 1);
        assertEquals("BL target", BASE + 8, bl.getProgramCounter().getCurrentAddress());
        assertEquals("BL link register", BASE + 4, bl.getRegisterController().readRegister(30));
    }

    private static void testSignedWordLoad(InstructionConfigLoader loader) throws Exception {
        SimulatorEngine engine = engine(loader,
            "MOVZ X10, #0x50, LSL #16",
            "MOVZ X2, #0xFFFF, LSL #16",
            "MOVK X2, #0xFFFF",
            "STURW X2, [X10, #0]",
            "LDURSW X3, [X10, #0]"
        );
        execute(engine, 5);
        assertEquals("LDURSW sign extension", -1L, engine.getRegisterController().readRegister(3));
    }

    public static void main(String[] args) throws Exception {
        InstructionConfigLoader loader = new InstructionConfigLoader();
        assertTrue("configuration loads", loader.loadConfig("resources/config/instructions.csv"));
        InstructionFactory.initialize(loader);

        testAssemblerAndDecoder(loader);
        testConditionalBranches(loader);
        testLogicalOpcodes(loader);
        testBranchAndLink(loader);
        testSignedWordLoad(loader);

        System.out.println("Regression tests passed: " + assertions + " assertions");
    }
}
