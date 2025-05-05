/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.instructions;

import legv8.exceptions.AssemblyException;
import legv8.exceptions.InvalidInstructionException;
import legv8.storage.*;
import legv8.util.ColoredLog;

import java.util.BitSet;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * InstructionFactory is a factory class responsible for creating instruction objects from bytecode or assembly lines.
 * It uses an InstructionConfigLoader to load instruction definitions and formats.
 */
public class InstructionFactory {
    // --- Constants ---
    // The opcode value for B.cond instructions is 0b01010100 (84 in decimal).
    private static final int BCOND_OPCODE_VALUE = 0b01010100; 

    // --- Fields ---
    // The InstructionConfigLoader instance used to load instruction definitions.
    private static InstructionConfigLoader configLoader;

    // --- Constructor ---
    /**
     * Private constructor to prevent instantiation.
     */
    // This class is a singleton and should not be instantiated directly.
    private InstructionFactory() {
    }

    // --- Static Methods ---

    /**
     * Initializes the InstructionFactory with a given InstructionConfigLoader.
     * @param loader The InstructionConfigLoader to use for loading instruction definitions.
     * @throws NullPointerException if the loader is null.
     */
    public static void initialize(InstructionConfigLoader loader) {
        configLoader = Objects.requireNonNull(loader, "ConfigLoader cannot be null for InstructionFactory.");
        System.out.println(ColoredLog.SUCCESS + "InstructionFactory initialized.");
    }

    /**
     * Checks if the InstructionFactory has been initialized.
     * @return true if initialized, false otherwise.
     */
    public static boolean isInitialized() {
        return configLoader != null;
    }

    /**
     * Creates an instruction object from the given bytecode.
     * @param bytecode The bytecode of the instruction as a BitSet.
     * @return An Instruction object representing the instruction.
     * @throws IllegalStateException if the factory is not initialized.
     * @throws NullPointerException if the bytecode is null.
     * @throws InvalidInstructionException if the bytecode cannot be decoded or no definition is found.
     */
    public static Instruction createFromBytecode(BitSet bytecode) {
        if (configLoader == null) throw new IllegalStateException("InstructionFactory not initialized.");
        Objects.requireNonNull(bytecode, "Bytecode cannot be null.");        

        InstructionDefinition definition = null;
        char identifiedFormat = '?';
        int opcodeId;

        // Check for B.cond instruction first
        opcodeId = Instruction.extractBits(bytecode, 24, 31);
        if (opcodeId == BCOND_OPCODE_VALUE) {    
            int condCode = Instruction.extractBits(bytecode, 0, 3); 
            String bCondMnemonic = "B." + getConditionMnemonic(condCode); 
            definition = configLoader.getDefinitionByMnemonic(bCondMnemonic); 

            System.out.printf("%s  Factory Debug: B.cond detected. Mnem='%s'. Definition found: %s\n",
                                        ColoredLog.INFO, bCondMnemonic, (definition != null ? definition.getMnemonic() : "NULL")); 
            
            if (definition != null && definition.getFormat() == 'C') { 
                identifiedFormat = 'C';
                System.out.println(ColoredLog.INFO + "  Factory Decode Hint: Identified B.cond -> " + bCondMnemonic);
            } else {
                System.err.printf("%sFATAL Factory Error: Missing or incorrect definition for %s (Cond: %d) in config.\n", ColoredLog.ERROR, bCondMnemonic, condCode);
                throw new InvalidInstructionException("Missing or invalid definition for " + bCondMnemonic);
            }
        } else {
            definition = configLoader.getDefinition(opcodeId, 'C');
            if (definition != null) {
                identifiedFormat = 'C';
            }
        }

        // Check for IM format 
        if (identifiedFormat == '?') { 
            opcodeId = Instruction.extractBits(bytecode, 23, 31);
            definition = configLoader.getDefinition(opcodeId, 'M');
            if (definition != null) identifiedFormat = 'M';
        }

        // Check for I format
        if (identifiedFormat == '?') { 
            opcodeId = Instruction.extractBits(bytecode, 22, 31);
            definition = configLoader.getDefinition(opcodeId, 'I');
            if (definition != null) identifiedFormat = 'I';
        }

        // Check for D format
        if (identifiedFormat == '?') { 
            opcodeId = Instruction.extractBits(bytecode, 21, 31);
            definition = configLoader.getDefinition(opcodeId, 'D');
            if (definition != null) identifiedFormat = 'D';
        }

        // Check for R format
        if (identifiedFormat == '?') { 
            opcodeId = Instruction.extractBits(bytecode, 21, 31);
            definition = configLoader.getDefinition(opcodeId, 'R');
            if (definition != null) identifiedFormat = 'R';
        }

        // Check for B format
        if (identifiedFormat == '?') { 
            opcodeId = Instruction.extractBits(bytecode, 26, 31);
            definition = configLoader.getDefinition(opcodeId, 'B');
            if (definition != null) identifiedFormat = 'B';
        }

        // Definition not found
        if (definition == null) {
            String bitsStr = Instruction.formatBitSet(bytecode);
            throw new InvalidInstructionException("Could not decode instruction or find definition for bytecode: " + bitsStr);
        }

        try {
            return switch (definition.getFormat()) {
                case 'R' -> new RFormatInstruction(bytecode, definition);
                case 'I' -> new IFormatInstruction(bytecode, definition);
                case 'D' -> new DFormatInstruction(bytecode, definition);
                case 'B' -> new BFormatInstruction(bytecode, definition);
                case 'C' -> new CBFormatInstruction(bytecode, definition); 
                case 'M' -> new IMFormatInstruction(bytecode, definition);
                default -> throw new InvalidInstructionException("Unsupported format '" + definition.getFormat() + "' found for mnemonic " + definition.getMnemonic());
            };
        } catch (Exception e) {
            throw new InvalidInstructionException("Error creating instruction object for " + definition.getMnemonic() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Converts a condition code to its mnemonic representation.
     * @param condCode The condition code (0-15).
     * @return The mnemonic representation of the condition code.
     */    
    private static String getConditionMnemonic(int condCode) {
        condCode &= 0xF; 
        return switch (condCode) {
            case 0b0000 -> "EQ"; case 0b0001 -> "NE"; case 0b0010 -> "HS"; case 0b0011 -> "LO";
            case 0b0100 -> "MI"; case 0b0101 -> "PL"; case 0b0110 -> "VS"; case 0b0111 -> "VC";
            case 0b1000 -> "HI"; case 0b1001 -> "LS"; case 0b1010 -> "GE"; case 0b1011 -> "LT";
            case 0b1100 -> "GT"; case 0b1101 -> "LE";
            default -> "??"; 
        };
    }
    
    /**
     * Creates an instruction object from the given assembly line.
     * @param assemblyLine The assembly line to assemble.
     * @param symbolTable The symbol table for label resolution.
     * @param currentInstructionAddress The current instruction address for branch target resolution.
     * @return An Instruction object representing the assembled instruction.
     * @throws IllegalStateException if the factory is not initialized.
     * @throws NullPointerException if any of the parameters are null.
     * @throws AssemblyException if the assembly line cannot be assembled or contains errors.
     */
    public static Instruction createFromAssembly(String assemblyLine, Map<String, Long> symbolTable, long currentInstructionAddress) {
        if (configLoader == null) throw new IllegalStateException("InstructionFactory not initialized.");
        Objects.requireNonNull(assemblyLine, "Assembly line cannot be null.");
        Objects.requireNonNull(symbolTable, "Symbol table cannot be null.");

        String trimmedLine = assemblyLine.trim();
        if (trimmedLine.isEmpty()) {
            throw new AssemblyException("Cannot assemble empty line.");
        }
 
        String[] parts = trimmedLine.split("\\s+", 2);
        String mnemonic = parts[0].toUpperCase();
        String operandsStr = (parts.length > 1) ? parts[1].trim() : "";

        InstructionDefinition def = configLoader.getDefinitionByMnemonic(mnemonic);
        if (def == null) {
            throw new AssemblyException("Unknown mnemonic: '" + mnemonic + "' in line: " + assemblyLine);
        }       

        BitSet bytecode = new BitSet(32);
        int opcode = def.getOpcodeIdentifierValue();
        if (opcode == -1) throw new AssemblyException("Internal error: Invalid opcode identifier for " + mnemonic);

        try {   
            switch (def.getFormat()) {
                case 'R':
                    assembleRFormat(bytecode, opcode, mnemonic, operandsStr);
                    break;
                case 'I':
                    assembleIFormat(bytecode, opcode, mnemonic, operandsStr);
                    break;
                case 'D':
                    assembleDFormat(bytecode, opcode, mnemonic, operandsStr);
                    break;
                case 'B':
                    assembleBFormat(bytecode, opcode, mnemonic, operandsStr, symbolTable, currentInstructionAddress);
                    break;
                case 'C':
                    assembleCBFormat(bytecode, opcode, mnemonic, operandsStr, symbolTable, currentInstructionAddress);
                    break;
                case 'M':
                    assembleIMFormat(bytecode, opcode, mnemonic, operandsStr);
                    break;
                default:
                    throw new AssemblyException("Assembly not implemented for format '" + def.getFormat() + "'");
            }
            
            return createFromBytecode(bytecode);
        } catch (AssemblyException ae) {
            throw new AssemblyException("Error assembling line: '" + assemblyLine + "' - " + ae.getMessage(), ae);
        } catch (Exception e) {
            throw new AssemblyException("Unexpected error assembling line: '" + assemblyLine + "' - " + e.getMessage(), e);
        }
    }

    // --- Assembly Methods ---

    /**
     * Assembles an R-format instruction.
     * @param bits The BitSet to store the instruction bits.
     * @param opcode The opcode value.
     * @param mnemonic The mnemonic of the instruction.
     * @param operands The operands of the instruction.
     */    
    private static void assembleRFormat(BitSet bits, int opcode, String mnemonic, String operands) {
        int rd, rn, rm = 0, shamt = 0;
        String[] ops = splitOperands(operands);

        switch (mnemonic) {
            case "LSL", "LSR", "ASR": 
                if (ops.length != 3) throw new AssemblyException(mnemonic + " requires 3 operands: Rd, Rn, #shamt");
                rd = parseRegister(ops[0]);
                rn = parseRegister(ops[1]);
                shamt = parseImmediate(ops[2]);
                if (shamt < 0 || shamt > 63) throw new AssemblyException("Shift amount (#" + shamt + ") out of range (0-63)");
                rm = 0; 
                break;
            case "BR": 
                if (ops.length != 1) throw new AssemblyException(mnemonic + " requires 1 operand: Rn");
                rn = parseRegister(ops[0]);
                rd = 0; rm = 0; shamt = 0; 
                break;
            default: 
                    if (ops.length != 3) throw new AssemblyException(mnemonic + " requires 3 operands: Rd, Rn, Rm");
                    rd = parseRegister(ops[0]);
                    rn = parseRegister(ops[1]);
                    rm = parseRegister(ops[2]);
                    shamt = 0; 
                break;
        }

        Instruction.setBits(bits, opcode, 21, 31);
        Instruction.setBits(bits, rm, 16, 20);
        Instruction.setBits(bits, shamt, 10, 15);
        Instruction.setBits(bits, rn, 5, 9);
        Instruction.setBits(bits, rd, 0, 4);
    }

    /**
     * Assembles an I-format instruction.
     * @param bits The BitSet to store the instruction bits.
     * @param opcode The opcode value.
     * @param mnemonic The mnemonic of the instruction.
     * @param operands The operands of the instruction.
     */    
    private static void assembleIFormat(BitSet bits, int opcode, String mnemonic, String operands) {
        String[] ops = splitOperands(operands);
        if (ops.length != 3) throw new AssemblyException(mnemonic + " requires 3 operands: Rd, Rn, #immediate");
        int rd = parseRegister(ops[0]);
        int rn = parseRegister(ops[1]);
        int imm12 = parseImmediate(ops[2]);
        
        if (imm12 < -2048 || imm12 > 2047) { 
            throw new AssemblyException("Immediate value (#" + imm12 + ") out of 12-bit signed range [-2048, 2047]");
        }

        Instruction.setBits(bits, opcode, 22, 31);
        Instruction.setBits(bits, imm12 & 0xFFF, 10, 21); 
        Instruction.setBits(bits, rn, 5, 9);
        Instruction.setBits(bits, rd, 0, 4);
    }

    /**
     * Assembles a D-format instruction.
     * @param bits The BitSet to store the instruction bits.
     * @param opcode The opcode value.
     * @param mnemonic The mnemonic of the instruction.
     * @param operands The operands of the instruction.
     */
    private static void assembleDFormat(BitSet bits, int opcode, String mnemonic, String operands) { 
        final Pattern D_FORMAT_ADDR_PATTERN = Pattern.compile("\\s*\\[\\s*(\\w+)\\s*,\\s*(#?-?\\w+)\\s*\\]\\s*");
        String[] ops = splitOperands(operands, 2); 
        if (ops.length != 2) throw new AssemblyException(mnemonic + " requires 2 operands: Rt, [Rn, #imm]");
        int rt = parseRegister(ops[0]);

        Matcher matcher = D_FORMAT_ADDR_PATTERN.matcher(ops[1]);
        if (!matcher.matches()) {
            throw new AssemblyException("Invalid D-format memory operand format: '" + ops[1] + "'. Expected [Rn, #imm]");
        }

        int rn = parseRegister(matcher.group(1));
        int imm9 = parseImmediate(matcher.group(2));
        if (imm9 < 0 || imm9 > 511) { 
            throw new AssemblyException("D-format offset (#" + imm9 + ") out of 9-bit unsigned range [0, 511]");
        }

        Instruction.setBits(bits, opcode, 21, 31);
        Instruction.setBits(bits, imm9 & 0x1FF, 12, 20); 
        Instruction.setBits(bits, 0, 10, 11); // Op2 field (unused for LDUR/STUR) set to 0
        Instruction.setBits(bits, rn, 5, 9);
        Instruction.setBits(bits, rt, 0, 4);
    }

    /**
     * Assembles a B-format instruction.
     * @param bits The BitSet to store the instruction bits.
     * @param opcode The opcode value.
     * @param mnemonic The mnemonic of the instruction.
     * @param operands The operands of the instruction.
     * @param symbolTable The symbol table for label resolution.
     * @param currentAddr The current instruction address for branch target resolution.
     */
    private static void assembleBFormat(BitSet bits, int opcode, String mnemonic, String operands, Map<String, Long> symbolTable, long currentAddr) {
        String target = operands.trim();
        if (target.isEmpty()) throw new AssemblyException(mnemonic + " requires a target label or offset");

        int offset26 = resolveBranchTarget(target, symbolTable, currentAddr, 26);

        Instruction.setBits(bits, opcode, 26, 31);
        Instruction.setBits(bits, offset26 & 0x3FFFFFF, 0, 25); 
    }

    /**
     * Assembles a CB-format instruction.
     * @param bits The BitSet to store the instruction bits.
     * @param opcode The opcode value.
     * @param mnemonic The mnemonic of the instruction.
     * @param operands The operands of the instruction.
     * @param symbolTable The symbol table for label resolution.
     * @param currentAddr The current instruction address for branch target resolution.
     */    
    private static void assembleCBFormat(BitSet bits, int opcode, String mnemonic, String operands, Map<String, Long> symbolTable, long currentAddr) {
        String[] ops = splitOperands(operands);
        int rt_or_cond;
        String targetStr;

        if (mnemonic.startsWith("B.")) { 
            if (ops.length != 1) throw new AssemblyException(mnemonic + " requires 1 operand: target");
            rt_or_cond = parseConditionCode(mnemonic); 
            targetStr = ops[0];
        } else { // CBZ/CBNZ Rt, target
            if (ops.length != 2) throw new AssemblyException(mnemonic + " requires 2 operands: Rt, target");
            rt_or_cond = parseRegister(ops[0]);
            targetStr = ops[1];
        }

        int offset19 = resolveBranchTarget(targetStr, symbolTable, currentAddr, 19);

        Instruction.setBits(bits, opcode, 24, 31);
        Instruction.setBits(bits, offset19 & 0x7FFFF, 5, 23); 
        Instruction.setBits(bits, rt_or_cond & 0x1F, 0, 4); 
    }

    /** 
     * Assembles an IM-format instruction.
     * @param bits The BitSet to store the instruction bits.
     * @param opcode The opcode value.
     * @param mnemonic The mnemonic of the instruction.
     * @param operands The operands of the instruction.
     */
    private static void assembleIMFormat(BitSet bits, int opcode, String mnemonic, String operands) {
        final Pattern IM_FORMAT_SHIFT_PATTERN = Pattern.compile("(.*?)(?:,\\s*(LSL)\\s*(#\\d+))?\\s*$", Pattern.CASE_INSENSITIVE);

        String[] ops = splitOperands(operands, 2); 
        if (ops.length != 2) throw new AssemblyException(mnemonic + " requires at least Rd, #imm operands");
        int rd = parseRegister(ops[0]);
        String immAndShiftPart = ops[1];

        Matcher matcher = IM_FORMAT_SHIFT_PATTERN.matcher(immAndShiftPart);
        if (!matcher.matches()) {
            throw new AssemblyException("Could not parse immediate/shift part for " + mnemonic + ": '" + immAndShiftPart + "'");
        }

        String immStr = matcher.group(1).trim(); 
        int imm16 = parseImmediate(immStr);
        int shiftVal = 0;
        int hw = 0;

        if (matcher.group(2) != null) { 
            String shiftStr = matcher.group(3);
            shiftVal = parseImmediate(shiftStr);
            
            if (shiftVal != 0 && shiftVal != 16 && shiftVal != 32 && shiftVal != 48) {
                throw new AssemblyException("Invalid LSL shift amount for " + mnemonic + ": #" + shiftVal + ". Must be 0, 16, 32, or 48.");
            }
            
            hw = shiftVal / 16;
        } 

        if (imm16 < 0 || imm16 > 65535) { 
            throw new AssemblyException("Immediate value (#" + imm16 + ") out of 16-bit unsigned range [0, 65535]");
        }

        Instruction.setBits(bits, opcode, 23, 31);
        Instruction.setBits(bits, hw & 0x3, 21, 22);       
        Instruction.setBits(bits, imm16 & 0xFFFF, 5, 20); 
        Instruction.setBits(bits, rd, 0, 4);
    }  
    
    // --- Helper Methods ---

    /**
     * Splits the operands string into an array of operands.
     * @param operands The operands string to split.
     * @return An array of operands.
     */
    private static String[] splitOperands(String operands) {
        if (operands == null || operands.trim().isEmpty()) {
            return new String[0];
        }
        return operands.trim().split("\\s*,\\s*");
    }

    /**
     * Splits the operands string into an array of operands with a specified count.
     * @param operands The operands string to split.
     * @param count The expected number of operands.
     * @return An array of operands.
     * @throws AssemblyException if the number of operands does not match the expected count.
     */
    private static String[] splitOperands(String operands, int count) {
        if (operands == null) {
            if (count == 0) return new String[0];
            else throw new AssemblyException("Expected " + count + " operands, but got none.");
        }
        
        String[] parts = operands.trim().split("\\s*,\\s*", count);
        if (parts.length != count || (parts.length == 1 && parts[0].isEmpty() && count > 0)) {
            if (parts.length == 1 && parts[0].isEmpty() && count == 1) {
                throw new AssemblyException("Expected " + count + " operand, but got empty string.");
            }

            if (parts.length < count) { 
                throw new AssemblyException("Expected " + count + " operands, but found fewer in '" + operands + "'");
            }
            
            if (parts.length == count && parts[count - 1].trim().isEmpty() && count > 0) {
                throw new AssemblyException("Trailing comma or missing operand detected in '" + operands + "'");
            }            
        }
        return parts;
    }

    /**
     * Parses a register operand and returns its index.
     * @param reg The register operand string.
     * @return The index of the register.
     * @throws AssemblyException if the register format is invalid or out of range.
     */    
    private static int parseRegister(String reg) {
        if (reg == null) throw new AssemblyException("Register operand is null.");
        reg = reg.trim().toUpperCase();
        if (reg.equals("XZR")) return RegisterStorage.ZERO_REGISTER_INDEX; 
        if (reg.equals("SP")) return 28; 

        if (!reg.startsWith("X") || reg.length() <= 1) {
            throw new AssemblyException("Invalid register format: '" + reg + "'. Expected X0-X30, XZR, or SP.");
        }
       
        try {
            int n = Integer.parseInt(reg.substring(1));
            if (n < 0 || n > 31) { 
                throw new AssemblyException("Register number out of range (0-31): '" + reg + "'");
            }
            
            if (n == RegisterStorage.ZERO_REGISTER_INDEX) {
                throw new AssemblyException("Use 'XZR' instead of 'X31' for the zero register.");         
            }

            return n;
        } catch (NumberFormatException e) {
            throw new AssemblyException("Invalid register number format: '" + reg + "'");
        }
    }

    /**
     * Parses an immediate operand and returns its value.
     * @param imm The immediate operand string.
     * @return The integer value of the immediate operand.
     * @throws AssemblyException if the immediate format is invalid or out of range.
     */
    private static int parseImmediate(String imm) {
        if (imm == null) throw new AssemblyException("Immediate operand is null.");
        imm = imm.trim();
        if (!imm.startsWith("#")) {
            throw new AssemblyException("Immediate value must start with '#': '" + imm + "'");
        }

        String valueStr = imm.substring(1).trim();
        if (valueStr.isEmpty()) {
            throw new AssemblyException("Empty immediate value after '#': '" + imm + "'");
        }
        
        if (valueStr.toUpperCase().endsWith("L")) {
            valueStr = valueStr.substring(0, valueStr.length() - 1);
        }
        
        try {
            // Integer.decode handles decimal, 0x/0X hex, and 0 octal prefixes
            return Integer.decode(valueStr);
        } catch (NumberFormatException e) {
            throw new AssemblyException("Invalid immediate value format: '" + valueStr + "' from '" + imm + "'", e);
        }
    }

    /**
     * Resolves the branch target address for a given label or immediate value.
     * @param target The target label or immediate value.
     * @param symbolTable The symbol table for label resolution.
     * @param currentAddr The current instruction address for branch target resolution.
     * @param offsetBits The number of bits for the offset (e.g., 26 for B-format).
     * @return The resolved branch target offset in terms of instructions (bytes/4).
     * @throws AssemblyException if the target cannot be resolved or is out of range.
     */    
    private static int resolveBranchTarget(String target, Map<String, Long> symbolTable, long currentAddr, int offsetBits) {
        target = target.trim();
        int instructionOffset; // Offset in terms of instructions (bytes/4)

        try {
            if (target.startsWith("#")) { 
                instructionOffset = parseImmediate(target);         
            } else { 
                if (!symbolTable.containsKey(target)) {
                    throw new AssemblyException("Undefined label: '" + target + "'");
                }
        
                long targetAddr = symbolTable.get(target);
                long byteOffset = targetAddr - currentAddr;

                if (byteOffset % 4 != 0) {
                    throw new AssemblyException("Branch target '" + target + "' (0x" + Long.toHexString(targetAddr) + ") is not word-aligned relative to PC (0x" + Long.toHexString(currentAddr) + ")");
                }
                instructionOffset = (int)(byteOffset / 4);
            }
        } catch (AssemblyException ae) {
            throw new AssemblyException("Error resolving branch target '" + target + "': " + ae.getMessage(), ae);
        }
        
        long maxOffset = (1L << (offsetBits - 1)) - 1; 
        long minOffset = -(1L << (offsetBits - 1));   

        if (instructionOffset < minOffset || instructionOffset > maxOffset) {
            throw new AssemblyException("Branch offset for target '" + target + "' (" + instructionOffset
                                    + ") exceeds " + offsetBits + "-bit signed range [" + minOffset + ", " + maxOffset + "].");
        }
        
        return instructionOffset;
    }


    /**
     * Parses a condition code from a mnemonic string.
     * @param mnemonic The mnemonic string (e.g., "B.EQ", "B.NE", etc.).
     * @return The condition code as an integer.
     * @throws AssemblyException if the mnemonic format is invalid or the condition code is unknown.
     */
    private static int parseConditionCode(String mnemonic) {
        if (!mnemonic.startsWith("B.") || mnemonic.length() <= 2) {
            throw new AssemblyException("Invalid B.cond mnemonic format: " + mnemonic);
        }
       
        String cond = mnemonic.substring(2);
        return switch (cond) {
            case "EQ" -> 0b0000; case "NE" -> 0b0001;
            case "CS", "HS" -> 0b0010; // Carry Set / Unsigned Higher or Same
            case "CC", "LO" -> 0b0011; // Carry Clear / Unsigned Lower
            case "MI" -> 0b0100; // Minus / Negative
            case "PL" -> 0b0101; // Plus / Positive or Zero
            case "VS" -> 0b0110; 
            case "VC" -> 0b0111; 
            case "HI" -> 0b1000; 
            case "LS" -> 0b1001; 
            case "GE" -> 0b1010; 
            case "LT" -> 0b1011; 
            case "GT" -> 0b1100; 
            case "LE" -> 0b1101; 
            
            default -> throw new AssemblyException("Unknown condition code suffix: '" + cond + "' in " + mnemonic);
        };
    }
}
