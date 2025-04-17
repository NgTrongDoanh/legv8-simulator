package simulator.instructions;

import simulator.exceptions.AssemblyException;
import simulator.exceptions.InvalidInstructionException;
import java.util.BitSet;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import simulator.storage.*;

/**
 * Factory class to create Instruction objects from bytecode or assembly strings.
 * Requires an initialized InstructionConfigLoader.
 */
public class InstructionFactory {
    private static final int BCOND_OPCODE_VALUE = 0b01010100; // 84 decimal

    private static InstructionConfigLoader configLoader;

    /** Initializes the factory with a config loader. Must be called before use. */
    public static void initialize(InstructionConfigLoader loader) {
        Objects.requireNonNull(loader, "ConfigLoader cannot be null for InstructionFactory.");
        configLoader = loader;
        System.out.println("InstructionFactory initialized.");
    }

    // *** THÊM HÀM KIỂM TRA ***
    public static boolean isInitialized() {
        return configLoader != null;
    }

    /**
     * Creates an Instruction object from its 32-bit bytecode.
     * Determines format and definition using the config loader.
     *
     * @throws InvalidInstructionException if bytecode cannot be decoded or no definition is found.
     * @throws IllegalStateException if the factory is not initialized.
     */
    public static Instruction createFromBytecode(BitSet bytecode) {
        if (configLoader == null) throw new IllegalStateException("InstructionFactory not initialized.");
        Objects.requireNonNull(bytecode, "Bytecode cannot be null.");

        // --- Logic to determine format and definition ---
        // Strategy: Try matching opcode patterns from most specific/longest to least specific.
        // Note: R and D formats share the same 11 bits for opcode ID, requiring careful handling.

        InstructionDefinition definition = null;
        char identifiedFormat = '?';
        int opcodeId;

        // --- Logic dò tìm format và definition MỚI ---

        // 1. Xử lý đặc biệt cho B.cond và CB Format (8 bits: 31-24)
        opcodeId = Instruction.extractBits(bytecode, 24, 31);
        if (opcodeId == BCOND_OPCODE_VALUE) {
            // --- Đây là lệnh B.cond ---
            int condCode = Instruction.extractBits(bytecode, 0, 3); // Lấy 4 bit điều kiện
            String bCondMnemonic = "B." + getConditionMnemonic(condCode); // Tạo mnemonic B.EQ, B.NE...
            definition = configLoader.getDefinitionByMnemonic(bCondMnemonic); // Tra cứu bằng mnemonic đầy đủ
            System.out.printf("  Factory Debug: B.cond detected. Mnem='%s'. Definition found: %s\n",
                  bCondMnemonic, (definition != null ? definition.getMnemonic() : "NULL")); // Thêm log này
            if (definition != null && definition.getFormat() == 'C') { // Đảm bảo định nghĩa tìm được đúng format
                identifiedFormat = 'C';
                 System.out.println("  Factory Decode Hint: Identified B.cond -> " + bCondMnemonic);
            } else {
                 // Lỗi: Không tìm thấy definition cho B.cond cụ thể trong config, hoặc format sai
                 System.err.printf("FATAL Factory Error: Missing or incorrect definition for %s (Cond: %d) in config.\n", bCondMnemonic, condCode);
                 // Có thể ném lỗi ở đây hoặc fallback (nhưng fallback sẽ sai)
                  throw new InvalidInstructionException("Missing or invalid definition for " + bCondMnemonic);
            }
        } else {
             // --- Không phải B.cond, thử CBZ/CBNZ ---
            definition = configLoader.getDefinition(opcodeId, 'C');
            if (definition != null) {
                identifiedFormat = 'C';
                // System.out.println("  Factory Decode Hint: Identified CBZ/CBNZ");
            }
        }


        // 2. Nếu không phải CB, thử các format khác (logic như cũ)
        if (identifiedFormat == '?') { // IM (9 bits: 31-23)
            opcodeId = Instruction.extractBits(bytecode, 23, 31);
            definition = configLoader.getDefinition(opcodeId, 'M');
            if (definition != null) identifiedFormat = 'M';
        }
        if (identifiedFormat == '?') { // I (10 bits: 31-22)
            opcodeId = Instruction.extractBits(bytecode, 22, 31);
            definition = configLoader.getDefinition(opcodeId, 'I');
            if (definition != null) identifiedFormat = 'I';
        }
        if (identifiedFormat == '?') { // D (11 bits: 31-21)
            opcodeId = Instruction.extractBits(bytecode, 21, 31);
            definition = configLoader.getDefinition(opcodeId, 'D');
            if (definition != null) identifiedFormat = 'D';
        }
        if (identifiedFormat == '?') { // R (11 bits: 31-21)
            opcodeId = Instruction.extractBits(bytecode, 21, 31);
            definition = configLoader.getDefinition(opcodeId, 'R');
            if (definition != null) identifiedFormat = 'R';
        }
        if (identifiedFormat == '?') { // B (6 bits: 31-26)
            opcodeId = Instruction.extractBits(bytecode, 26, 31);
            definition = configLoader.getDefinition(opcodeId, 'B');
            if (definition != null) identifiedFormat = 'B';
        }

        // --- Final Check and Instantiation (như cũ) ---
        if (definition == null) {
            String bitsStr = Instruction.formatBitSet(bytecode);
            throw new InvalidInstructionException("Could not decode instruction or find definition for bytecode: " + bitsStr);
        }

        try {
            // Tạo đối tượng instruction dựa trên definition cuối cùng tìm được
            return switch (definition.getFormat()) {
                case 'R' -> new RFormatInstruction(bytecode, definition);
                case 'I' -> new IFormatInstruction(bytecode, definition);
                case 'D' -> new DFormatInstruction(bytecode, definition);
                case 'B' -> new BFormatInstruction(bytecode, definition);
                case 'C' -> new CBFormatInstruction(bytecode, definition); // Dùng chung cho B.cond, CBZ, CBNZ
                case 'M' -> new IMFormatInstruction(bytecode, definition);
                default -> throw new InvalidInstructionException("Unsupported format '" + definition.getFormat() + "' found for mnemonic " + definition.getMnemonic());
            };
        } catch (Exception e) {
             throw new InvalidInstructionException("Error creating instruction object for " + definition.getMnemonic() + ": " + e.getMessage(), e);
        }
    }

    // Helper lấy tên điều kiện (đảm bảo đã có trong Factory)
    private static String getConditionMnemonic(int condCode) {
        condCode &= 0xF; // Mask 4 bits
        return switch (condCode) {
                case 0b0000 -> "EQ"; case 0b0001 -> "NE"; case 0b0010 -> "CS"; /*HS*/ case 0b0011 -> "CC"; /*LO*/
                case 0b0100 -> "MI"; case 0b0101 -> "PL"; case 0b0110 -> "VS"; case 0b0111 -> "VC";
                case 0b1000 -> "HI"; case 0b1001 -> "LS"; case 0b1010 -> "GE"; case 0b1011 -> "LT";
                case 0b1100 -> "GT"; case 0b1101 -> "LE";
                default -> "??"; // Invalid condition code
        };
    }



    /**
     * Creates an Instruction object from a single line of assembly code.
     * Requires symbol table for resolving labels.
     *
     * @param assemblyLine The assembly code line (assumed preprocessed - comments/whitespace removed).
     * @param symbolTable Map of label names to their byte addresses.
     * @param currentInstructionAddress The byte address where this instruction will reside.
     * @return The created Instruction object.
     * @throws AssemblyException If parsing fails, label is undefined, immediate is out of range, etc.
     * @throws IllegalStateException if the factory is not initialized.
     */
    public static Instruction createFromAssembly(String assemblyLine, Map<String, Long> symbolTable, long currentInstructionAddress) {
        if (configLoader == null) throw new IllegalStateException("InstructionFactory not initialized.");
        Objects.requireNonNull(assemblyLine, "Assembly line cannot be null.");
        Objects.requireNonNull(symbolTable, "Symbol table cannot be null.");

        String trimmedLine = assemblyLine.trim();
        if (trimmedLine.isEmpty()) {
            throw new AssemblyException("Cannot assemble empty line.");
        }

        // Split mnemonic and operands
        String[] parts = trimmedLine.split("\\s+", 2);
        String mnemonic = parts[0].toUpperCase();
        String operandsStr = (parts.length > 1) ? parts[1].trim() : "";

        InstructionDefinition def = configLoader.getDefinitionByMnemonic(mnemonic);
        if (def == null) {
            throw new AssemblyException("Unknown mnemonic: '" + mnemonic + "' in line: " + assemblyLine);
        }

        // System.out.printf("Assembling [0x%X]: %s %s (Format %c)\n", currentInstructionAddress, mnemonic, operandsStr, def.getFormat());

        BitSet bytecode = new BitSet(32);
        int opcode = def.getOpcodeIdentifierValue();
        if (opcode == -1) throw new AssemblyException("Internal error: Invalid opcode identifier for " + mnemonic);

        try {
            // Assemble based on format
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
            // System.out.println("  Generated Bytecode: " + Instruction.formatBitSet(bytecode));

            // Once bytecode is assembled, use the standard createFromBytecode to get the final object
            // This ensures consistency between assembled and decoded instructions
            return createFromBytecode(bytecode);

        } catch (AssemblyException ae) {
            // Add context to the exception
            throw new AssemblyException("Error assembling line: '" + assemblyLine + "' - " + ae.getMessage(), ae);
        } catch (Exception e) {
            // Catch unexpected errors
            throw new AssemblyException("Unexpected error assembling line: '" + assemblyLine + "' - " + e.getMessage(), e);
        }
    }

    // --- Private Assembly Helper Methods ---

    private static void assembleRFormat(BitSet bits, int opcode, String mnemonic, String operands) {
        int rd, rn, rm = 0, shamt = 0;
        String[] ops = splitOperands(operands);

        switch (mnemonic) {
            case "LSL", "LSR", "ASR": // Rd, Rn, #shamt
                if (ops.length != 3) throw new AssemblyException(mnemonic + " requires 3 operands: Rd, Rn, #shamt");
                rd = parseRegister(ops[0]);
                rn = parseRegister(ops[1]);
                shamt = parseImmediate(ops[2]);
                if (shamt < 0 || shamt > 63) throw new AssemblyException("Shift amount (#" + shamt + ") out of range (0-63)");
                rm = 0; // Rm field is not used for shifts
                break;
            case "BR": // Rn
                if (ops.length != 1) throw new AssemblyException(mnemonic + " requires 1 operand: Rn");
                rn = parseRegister(ops[0]);
                rd = 0; rm = 0; shamt = 0; // Other fields are zero or ignored
                break;
            default: // Standard Rd, Rn, Rm
                    if (ops.length != 3) throw new AssemblyException(mnemonic + " requires 3 operands: Rd, Rn, Rm");
                    rd = parseRegister(ops[0]);
                    rn = parseRegister(ops[1]);
                    rm = parseRegister(ops[2]);
                    shamt = 0; // Shamt field is not used
                break;
        }

        Instruction.setBits(bits, opcode, 21, 31);
        Instruction.setBits(bits, rm, 16, 20);
        Instruction.setBits(bits, shamt, 10, 15);
        Instruction.setBits(bits, rn, 5, 9);
        Instruction.setBits(bits, rd, 0, 4);
    }

    private static void assembleIFormat(BitSet bits, int opcode, String mnemonic, String operands) {
        String[] ops = splitOperands(operands);
        if (ops.length != 3) throw new AssemblyException(mnemonic + " requires 3 operands: Rd, Rn, #immediate");
        int rd = parseRegister(ops[0]);
        int rn = parseRegister(ops[1]);
        int imm12 = parseImmediate(ops[2]);

        // Range check for 12-bit signed immediate
        if (imm12 < -2048 || imm12 > 2047) { // -(1<<11) to (1<<11)-1
            throw new AssemblyException("Immediate value (#" + imm12 + ") out of 12-bit signed range [-2048, 2047]");
        }

        Instruction.setBits(bits, opcode, 22, 31);
        Instruction.setBits(bits, imm12 & 0xFFF, 10, 21); // Mask to 12 bits before setting
        Instruction.setBits(bits, rn, 5, 9);
        Instruction.setBits(bits, rd, 0, 4);
    }

    // Regex pattern for D-format memory operand: [Rn, #imm] (allowing whitespace)
    private static final Pattern D_FORMAT_ADDR_PATTERN = Pattern.compile("\\s*\\[\\s*(\\w+)\\s*,\\s*(#?-?\\w+)\\s*\\]\\s*");

    private static void assembleDFormat(BitSet bits, int opcode, String mnemonic, String operands) {
        String[] ops = splitOperands(operands, 2); // Split into Rt and the rest "[Rn, #imm]"
        if (ops.length != 2) throw new AssemblyException(mnemonic + " requires 2 operands: Rt, [Rn, #imm]");
        int rt = parseRegister(ops[0]);

        Matcher matcher = D_FORMAT_ADDR_PATTERN.matcher(ops[1]);
        if (!matcher.matches()) {
                throw new AssemblyException("Invalid D-format memory operand format: '" + ops[1] + "'. Expected [Rn, #imm]");
        }
        int rn = parseRegister(matcher.group(1));
        int imm9 = parseImmediate(matcher.group(2));

        // Range check for 9-bit unsigned offset
        if (imm9 < 0 || imm9 > 511) { // 0 to (1<<9)-1
                throw new AssemblyException("D-format offset (#" + imm9 + ") out of 9-bit unsigned range [0, 511]");
        }

        Instruction.setBits(bits, opcode, 21, 31);
        Instruction.setBits(bits, imm9 & 0x1FF, 12, 20); // Mask to 9 bits
        Instruction.setBits(bits, 0, 10, 11); // Op2 field (unused for LDUR/STUR) set to 0
        Instruction.setBits(bits, rn, 5, 9);
        Instruction.setBits(bits, rt, 0, 4);
    }

    private static void assembleBFormat(BitSet bits, int opcode, String mnemonic, String operands, Map<String, Long> symbolTable, long currentAddr) {
        String target = operands.trim();
        if (target.isEmpty()) throw new AssemblyException(mnemonic + " requires a target label or offset");

        int offset26 = resolveBranchTarget(target, symbolTable, currentAddr, 26);

        Instruction.setBits(bits, opcode, 26, 31);
        Instruction.setBits(bits, offset26 & 0x3FFFFFF, 0, 25); // Mask to 26 bits
    }

    private static void assembleCBFormat(BitSet bits, int opcode, String mnemonic, String operands, Map<String, Long> symbolTable, long currentAddr) {
        String[] ops = splitOperands(operands);
        int rt_or_cond;
        String targetStr;

        if (mnemonic.startsWith("B.")) { // B.cond target
                if (ops.length != 1) throw new AssemblyException(mnemonic + " requires 1 operand: target");
                rt_or_cond = parseConditionCode(mnemonic); // Get cond code from mnemonic
                targetStr = ops[0];
        } else { // CBZ/CBNZ Rt, target
            if (ops.length != 2) throw new AssemblyException(mnemonic + " requires 2 operands: Rt, target");
            rt_or_cond = parseRegister(ops[0]);
            targetStr = ops[1];
        }

        int offset19 = resolveBranchTarget(targetStr, symbolTable, currentAddr, 19);

        Instruction.setBits(bits, opcode, 24, 31);
        Instruction.setBits(bits, offset19 & 0x7FFFF, 5, 23); // Mask to 19 bits
        Instruction.setBits(bits, rt_or_cond & 0x1F, 0, 4); // Mask to 5 bits
    }

        // Regex pattern for IM-format shift operand: , LSL #sh (optional)
    private static final Pattern IM_FORMAT_SHIFT_PATTERN = Pattern.compile("(.*?)(?:,\\s*(LSL)\\s*(#\\d+))?\\s*$", Pattern.CASE_INSENSITIVE);

    private static void assembleIMFormat(BitSet bits, int opcode, String mnemonic, String operands) {
        String[] ops = splitOperands(operands, 2); // Split Rd and the rest
        if (ops.length != 2) throw new AssemblyException(mnemonic + " requires at least Rd, #imm operands");
        int rd = parseRegister(ops[0]);
        String immAndShiftPart = ops[1];

        Matcher matcher = IM_FORMAT_SHIFT_PATTERN.matcher(immAndShiftPart);
        if (!matcher.matches()) {
            // Should generally match even without LSL part
            throw new AssemblyException("Could not parse immediate/shift part for " + mnemonic + ": '" + immAndShiftPart + "'");
        }

        String immStr = matcher.group(1).trim(); // Part before optional ", LSL #sh"
        int imm16 = parseImmediate(immStr);
        int shiftVal = 0;
        int hw = 0;

        if (matcher.group(2) != null) { // Optional LSL part exists
            String shiftStr = matcher.group(3);
            shiftVal = parseImmediate(shiftStr);
                if (shiftVal != 0 && shiftVal != 16 && shiftVal != 32 && shiftVal != 48) {
                throw new AssemblyException("Invalid LSL shift amount for " + mnemonic + ": #" + shiftVal + ". Must be 0, 16, 32, or 48.");
            }
                hw = shiftVal / 16;
        } // else shiftVal remains 0, hw remains 0

        // Range check for 16-bit unsigned immediate
            if (imm16 < 0 || imm16 > 65535) { // 0 to (1<<16)-1
                throw new AssemblyException("Immediate value (#" + imm16 + ") out of 16-bit unsigned range [0, 65535]");
            }

        Instruction.setBits(bits, opcode, 23, 31);
        Instruction.setBits(bits, hw & 0x3, 21, 22);       // Mask to 2 bits
        Instruction.setBits(bits, imm16 & 0xFFFF, 5, 20); // Mask to 16 bits
        Instruction.setBits(bits, rd, 0, 4);
    }

    // --- Parsing Helper Methods ---

    /** Splits operands by comma, trimming whitespace. */
    private static String[] splitOperands(String operands) {
        if (operands == null || operands.trim().isEmpty()) {
            return new String[0];
        }
        return operands.trim().split("\\s*,\\s*");
    }

    /** Splits operands by comma, ensuring exactly 'count' parts. */
    private static String[] splitOperands(String operands, int count) {
        if (operands == null) {
                if (count == 0) return new String[0];
                else throw new AssemblyException("Expected " + count + " operands, but got none.");
            }
            // Split by comma, allowing whitespace around it. Limit the split.
            String[] parts = operands.trim().split("\\s*,\\s*", count);
            if (parts.length != count || (parts.length == 1 && parts[0].isEmpty() && count > 0)) {
                // Handle edge case where split results in [""] for empty input
                if (parts.length == 1 && parts[0].isEmpty() && count == 1) {
                    throw new AssemblyException("Expected " + count + " operand, but got empty string.");
                }
                // Handle case where input is just commas, e.g., ",," -> ["", "", ""]
                int actualParts = 0;
                for (String p : parts) if (!p.isEmpty()) actualParts++;
                // This check might be too simple, need robust parsing if complex operands allowed
                // For now, basic length check
                if (parts.length < count) { // Check if fewer parts than expected due to limit
                    throw new AssemblyException("Expected " + count + " operands, but found fewer in '" + operands + "'");
                }
                // If split produced *more* parts than count (due to limit), the last part contains the rest
                // If it produced exactly count, it's likely okay.

                // Simple check: if split produced `count` items, assume okay for now
                // unless the *last* item is empty, which implies a trailing comma maybe?
                if (parts.length == count && parts[count - 1].trim().isEmpty() && count > 0) {
                    throw new AssemblyException("Trailing comma or missing operand detected in '" + operands + "'");
                }
                // This basic split might fail on complex operands, consider more robust regex if needed
            }
            return parts;
    }


    /** Parses register string (Xn, XZR, SP). Case-insensitive. */
    private static int parseRegister(String reg) {
        if (reg == null) throw new AssemblyException("Register operand is null.");
        reg = reg.trim().toUpperCase();
        if (reg.equals("XZR")) return RegisterStorage.ZERO_REGISTER_INDEX; // 31
        if (reg.equals("SP")) return 28; // Typically X28 is SP

        if (!reg.startsWith("X") || reg.length() <= 1) {
            throw new AssemblyException("Invalid register format: '" + reg + "'. Expected X0-X30, XZR, or SP.");
        }
        try {
            int n = Integer.parseInt(reg.substring(1));
            if (n < 0 || n > 31) { // Allow X31 only if it maps to XZR (handled above)
                throw new AssemblyException("Register number out of range (0-31): '" + reg + "'");
            }
            // Do not allow explicitly writing X31 if it wasn't caught by XZR check
            if (n == RegisterStorage.ZERO_REGISTER_INDEX) {
                // This case should already be handled by "XZR" check, but safeguard here.
                // If code reaches here, it means "X31" was used instead of "XZR".
                // Depending on strictness, could throw an error or return 31.
                // Let's be strict: use XZR for the zero register.
                    throw new AssemblyException("Use 'XZR' instead of 'X31' for the zero register.");
                // return RegisterStorage.ZERO_REGISTER_INDEX;
            }
            return n;
        } catch (NumberFormatException e) {
            throw new AssemblyException("Invalid register number format: '" + reg + "'");
        }
    }

    /** Parses immediate value string (#decimal, #0xHex, #0Octal). */
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
        // Allow trailing 'L' for long literals, remove it for Integer parsing
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

    /** Calculates instruction offset for branches. */
    private static int resolveBranchTarget(String target, Map<String, Long> symbolTable, long currentAddr, int offsetBits) {
        target = target.trim();
        int instructionOffset; // Offset in terms of instructions (bytes/4)

        try {
            if (target.startsWith("#")) { // Direct instruction offset
                instructionOffset = parseImmediate(target);
                // Assume #val is already instruction offset, not byte offset
            } else { // Label
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
            // Re-throw parsing errors with context
            throw new AssemblyException("Error resolving branch target '" + target + "': " + ae.getMessage(), ae);
        }

        // Range check for the instruction offset based on available bits
        long maxOffset = (1L << (offsetBits - 1)) - 1; // Max positive value (e.g., 2^25 - 1 for B)
        long minOffset = -(1L << (offsetBits - 1));   // Min negative value (e.g., -2^25 for B)

        if (instructionOffset < minOffset || instructionOffset > maxOffset) {
            throw new AssemblyException("Branch offset for target '" + target + "' (" + instructionOffset
                                    + ") exceeds " + offsetBits + "-bit signed range [" + minOffset + ", " + maxOffset + "].");
        }
        return instructionOffset;
    }

    /** Parses B.cond mnemonic suffix to get condition code. */
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
            case "VS" -> 0b0110; // Overflow Set
            case "VC" -> 0b0111; // Overflow Clear
            case "HI" -> 0b1000; // Unsigned Higher
            case "LS" -> 0b1001; // Unsigned Lower or Same
            case "GE" -> 0b1010; // Signed Greater Than or Equal
            case "LT" -> 0b1011; // Signed Less Than
            case "GT" -> 0b1100; // Signed Greater Than
            case "LE" -> 0b1101; // Signed Less Than or Equal
            // AL (Always) and NV (Never) usually don't have specific B.指令 forms
            default -> throw new AssemblyException("Unknown condition code suffix: '" + cond + "' in " + mnemonic);
        };
    }
}