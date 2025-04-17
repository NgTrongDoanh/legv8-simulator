package simulator.instructions;

import simulator.util.ALUOperation;
import simulator.util.ControlSignals;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Loads instruction definitions from a configuration file (e.g., CSV).
 * Builds maps for easy lookup by opcode or mnemonic.
 */
public class InstructionConfigLoader {

    // Key: Opcode Identifier Value (int), Value: Map<Format Char, Definition>
    private final Map<Integer, Map<Character, InstructionDefinition>> detailedDefinitionMap = new HashMap<>();
    // Key: Mnemonic (String, uppercase), Value: Definition (first one found for that mnemonic)
    private final Map<String, InstructionDefinition> mnemonicMap = new HashMap<>();

    /**
     * Loads the configuration from the specified resource path within the classpath.
     *
     * @param resourcePath Path to the config file (e.g., "/instructions_config.csv").
     * @return true if loading was successful, false otherwise.
     */
    public boolean loadConfig(String resourcePath) {
        detailedDefinitionMap.clear();
        mnemonicMap.clear();
        System.out.println("Loading instruction configuration from resource: " + resourcePath);

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
             if (is == null) {
                System.err.println("ConfigLoader FATAL ERROR: Resource not found: " + resourcePath);
                System.err.println("Ensure the file is in the correct location within your project's resources folder and the path starts with '/'.");
                return false;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(is)))) {
                String line;
                int lineNumber = 0;
                boolean headerSkipped = false;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;

                    // Simple header skip based on common keywords
                    if (!headerSkipped && (line.toLowerCase().contains("mnemonic") || line.toLowerCase().contains("opcode"))) {
                        headerSkipped = true;
                        continue;
                    }

                    String[] parts = line.split(",", -1); // -1 keeps trailing empty strings
                    // Expected columns: Mnemonic, Format, OpcodeID(Bin), RegW, ALUSrc, MemW, MemR, MemToReg, ZeroB, FlagB, UncondB, Reg2Loc(Ignore?), FlagW, ALUOperation(Enum)
                    if (parts.length != 14) { // Adjust count based on final CSV structure
                        System.err.printf("ConfigLoader WARNING line %d: Incorrect field count (%d, expected 14). Skipping: %s\n", lineNumber, parts.length, line);
                        continue;
                    }

                    try {
                        String mnemonic = parts[0].trim().toUpperCase();
                        String formatStr = parts[1].trim().toUpperCase();
                        String opcodeIdStr = parts[2].trim();
                        if (opcodeIdStr.isEmpty()) {
                            System.err.printf("ConfigLoader WARNING line %d: Empty Opcode ID for %s. Skipping.\n", lineNumber, mnemonic);
                            continue;
                        }

                        char formatChar = parseFormat(formatStr);
                        if (formatChar == '?') {
                             System.err.printf("ConfigLoader WARNING line %d: Unknown format '%s' for %s. Skipping.\n", lineNumber, formatStr, mnemonic);
                            continue;
                        }

                        int opcodeIdValue = Integer.parseInt(opcodeIdStr, 2);

                        // Parse control signals (indices 3 to 12)
                        boolean regWrite = parseFlag(parts[3], mnemonic, "RegW");
                        boolean aluSrc = parseFlag(parts[4], mnemonic, "ALUSrc");
                        boolean memWrite = parseFlag(parts[5], mnemonic, "MemW");
                        boolean memRead = parseFlag(parts[6], mnemonic, "MemR");
                        boolean memToReg = parseFlag(parts[7], mnemonic, "MemToReg");
                        boolean zeroBranch = parseFlag(parts[8], mnemonic, "ZeroB");
                        boolean flagBranch = parseFlag(parts[9], mnemonic, "FlagB");
                        boolean uncondBranch = parseFlag(parts[10], mnemonic, "UncondB");
                        boolean reg2Loc = parseFlag(parts[11], mnemonic, "Reg2Loc"); // Parse but might ignore later
                        boolean flagWrite = parseFlag(parts[12], mnemonic, "FlagW");

                        ControlSignals signals = new ControlSignals(regWrite, aluSrc, memWrite, memRead, memToReg,
                                                                    zeroBranch, flagBranch, uncondBranch, reg2Loc, flagWrite);

                        // Parse ALU Operation (index 13)
                        ALUOperation aluOperation;
                        try {
                            aluOperation = ALUOperation.valueOf(parts[13].trim().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            System.err.printf("ConfigLoader WARNING line %d: Invalid ALUOperation '%s' for %s. Using UNKNOWN.\n", lineNumber, parts[13].trim(), mnemonic);
                            aluOperation = ALUOperation.UNKNOWN;
                        }

                        // Create definition
                        InstructionDefinition definition = new InstructionDefinition(
                            mnemonic, formatChar, opcodeIdStr, signals, aluOperation
                        );

                        // Store in maps
                        // 1. Detailed map (OpcodeID -> Format -> Definition)
                        detailedDefinitionMap.computeIfAbsent(opcodeIdValue, k -> new HashMap<>()).put(formatChar, definition);

                        // 2. Mnemonic map (Mnemonic -> Definition) - Store only if not already present
                        mnemonicMap.putIfAbsent(mnemonic, definition);

                        // System.out.println("  Loaded: " + definition); // Optional debug log

                    } catch (NumberFormatException e) {
                         System.err.printf("ConfigLoader ERROR line %d: Invalid number format (OpcodeID?). Skipping: %s - %s\n", lineNumber, line, e.getMessage());
                    } catch (ArrayIndexOutOfBoundsException e) {
                         System.err.printf("ConfigLoader ERROR line %d: Missing fields. Skipping: %s\n", lineNumber, line);
                    } catch (Exception e) { // Catch other potential errors during parsing
                         System.err.printf("ConfigLoader ERROR line %d: Cannot parse line: %s - %s\n", lineNumber, line, e.getMessage());
                    }
                } // end while loop
// Trong InstructionConfigLoader.loadConfig(), sau vòng lặp while:
System.out.println("--- Mnemonic Map Contents ---");
mnemonicMap.forEach((mnem, def) -> System.out.printf("  '%s' -> %s\n", mnem, def.getMnemonic()));
System.out.println("---------------------------");

                System.out.printf("Instruction configuration loaded. %d unique mnemonics, %d opcode/format definitions.\n",
                                  mnemonicMap.size(), countTotalDefinitions());
                return true;
            } // BufferedReader closed here
        } catch (IOException | NullPointerException e) { // Catch errors opening stream or reading
            System.err.println("ConfigLoader FATAL ERROR: Cannot read config file resource '" + resourcePath + "': " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed debugging
            return false;
        }
    }

    private char parseFormat(String formatStr) {
        return switch (formatStr) {
            case "R" -> 'R';
            case "I" -> 'I';
            case "D" -> 'D';
            case "B" -> 'B';
            case "CB", "C" -> 'C'; // Allow C as alias for CB
            case "IM", "M" -> 'M'; // Allow M as alias for IM
            default -> '?'; // Unknown format
        };
    }

    private boolean parseFlag(String flagStr, String mnemonic, String flagName) {
        String trimmed = flagStr.trim();
        if (trimmed.equals("1")) {
            return true;
        } else if (trimmed.equals("0")) {
            return false;
        } else {
            // Allow 'X' or empty to mean 'don't care' or false (defaulting to false)
            // System.err.printf("ConfigLoader WARNING: Invalid flag value '%s' for %s/%s. Assuming 0 (false).\n", trimmed, mnemonic, flagName);
            return false;
        }
    }

     private int countTotalDefinitions() {
        int count = 0;
        for (Map<Character, InstructionDefinition> formatMap : detailedDefinitionMap.values()) {
            count += formatMap.size();
        }
        return count;
    }

    // --- Getters ---

    /** Gets the definition based on Opcode ID value and expected Format character. */
    public InstructionDefinition getDefinition(int opcodeId, char format) {
        Map<Character, InstructionDefinition> formatMap = detailedDefinitionMap.get(opcodeId);
        return (formatMap != null) ? formatMap.get(format) : null;
    }

    /** Gets the definition based on the instruction mnemonic (case-insensitive). */
    public InstructionDefinition getDefinitionByMnemonic(String mnemonic) {
        return mnemonicMap.get(mnemonic.toUpperCase());
    }

    /** Gets the entire mnemonic map (e.g., for assembler validation). */
    public Map<String, InstructionDefinition> getMnemonicMap() {
        return mnemonicMap; // Consider returning an unmodifiable view if needed
    }

     /** Tries to determine the format based *only* on the opcode ID.
      *  NOTE: This can be ambiguous if R/D formats share the same ID bits.
      *  It might return the first format found (e.g., 'D' before 'R').
      *  Use getDefinition(opcodeId, format) when format is known/expected.
      */
     public char inferFormatFromOpcodeId(int opcodeId) {
         Map<Character, InstructionDefinition> formatMap = detailedDefinitionMap.get(opcodeId);
         if (formatMap != null && !formatMap.isEmpty()) {
             // Simple approach: return the first format found.
             // Prioritize based on typical distinctness (B, CB, IM, I are often more unique)
             if (formatMap.containsKey('B')) return 'B';
             if (formatMap.containsKey('C')) return 'C';
             if (formatMap.containsKey('M')) return 'M';
             if (formatMap.containsKey('I')) return 'I';
             if (formatMap.containsKey('D')) return 'D'; // Check D before R if they overlap
             if (formatMap.containsKey('R')) return 'R';
             return formatMap.keySet().iterator().next(); // Fallback: return any found format
         }
         return '?'; // Not found
     }
}