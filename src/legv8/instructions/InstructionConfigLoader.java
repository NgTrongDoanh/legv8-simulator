package legv8.instructions;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.io.File;

import legv8.util.ColoredLog;
import legv8.util.ControlSignals;

public class InstructionConfigLoader {

    
    private final Map<Integer, Map<Character, InstructionDefinition>> detailedDefinitionMap = new HashMap<>();
    
    
    private final Map<String, InstructionDefinition> mnemonicMap = new HashMap<>();
    
    
    public InstructionConfigLoader() {
        
    }

    public InstructionDefinition getDefinition(int opcodeId, char format) {
        Map<Character, InstructionDefinition> formatMap = detailedDefinitionMap.get(opcodeId);
        return (formatMap != null) ? formatMap.get(format) : null;
    }
    
    public InstructionDefinition getDefinitionByMnemonic(String mnemonic) {
        return mnemonicMap.get(mnemonic.toUpperCase());
    }
    
    public Map<String, InstructionDefinition> getMnemonicMap() {
        return mnemonicMap; 
    }
    
    public boolean loadConfig(String resourcePath) {
        detailedDefinitionMap.clear();
        mnemonicMap.clear();
        System.out.println(ColoredLog.PENDING + "Loading instruction configuration from resource: " + resourcePath);
        try (InputStream is = new FileInputStream(resourcePath)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(is)))) {
                String line;
                int lineNumber = 0;
                boolean headerSkipped = false;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;

                    
                    if (!headerSkipped && (line.toLowerCase().contains("mnemonic") || line.toLowerCase().contains("opcode"))) {
                        headerSkipped = true;
                        continue;
                    }

                    String[] parts = line.split(",", -1); 
                    
                    if (parts.length != 15) { 
                        System.err.printf("%sConfigLoader WARNING line %d: Incorrect field count (%d, expected 14). Skipping: %s\n", ColoredLog.WARNING, lineNumber, parts.length, line);
                        continue;
                    }

                    try {
                        String mnemonic = parts[0].trim().toUpperCase();
                        String formatStr = parts[1].trim().toUpperCase();
                        String opcodeIdStr = parts[2].trim();
                        if (opcodeIdStr.isEmpty()) {
                            System.err.printf("%sConfigLoader WARNING line %d: Empty Opcode ID for %s. Skipping.\n", ColoredLog.WARNING, lineNumber, mnemonic);
                            continue;
                        }

                        char formatChar = parseFormat(formatStr);
                        if (formatChar == '?') {
                             System.err.printf("%sConfigLoader WARNING line %d: Unknown format '%s' for %s. Skipping.\n", ColoredLog.WARNING, lineNumber, formatStr, mnemonic);
                            continue;
                        }

                        int opcodeIdValue = Integer.parseInt(opcodeIdStr, 2);

                        
                        boolean regWrite = parseFlag(parts[3], mnemonic, "RegW");
                        boolean aluSrc = parseFlag(parts[4], mnemonic, "ALUSrc");
                        boolean memWrite = parseFlag(parts[5], mnemonic, "MemW");
                        boolean memRead = parseFlag(parts[6], mnemonic, "MemR");
                        boolean memToReg = parseFlag(parts[7], mnemonic, "MemToReg");
                        boolean zeroBranch = parseFlag(parts[8], mnemonic, "ZeroB");
                        boolean flagBranch = parseFlag(parts[9], mnemonic, "FlagB");
                        boolean uncondBranch = parseFlag(parts[10], mnemonic, "UncondB");
                        boolean reg2Loc = parseFlag(parts[11], mnemonic, "Reg2Loc"); 
                        boolean flagWrite = parseFlag(parts[12], mnemonic, "FlagW");
                        int aluOp = parseBinary(parts[13], mnemonic, "FlagW");
                        int operation = parseBinary(parts[14], mnemonic, "ALUOperation"); 

                        
                        
                        
                        
                        
                        
                        
                        

                        ControlSignals signals = new ControlSignals(
                            reg2Loc, uncondBranch, flagBranch, zeroBranch,
                            memRead, memToReg, memWrite,
                            flagWrite,
                            aluSrc, aluOp,
                            regWrite,
                            operation 
                        );


                        
                        InstructionDefinition definition = new InstructionDefinition(
                            mnemonic, formatChar, opcodeIdStr, signals
                        );

                        
                        
                        detailedDefinitionMap.computeIfAbsent(opcodeIdValue, k -> new HashMap<>()).put(formatChar, definition);

                        
                        mnemonicMap.putIfAbsent(mnemonic, definition);

                        

                    } catch (NumberFormatException e) {
                         System.err.printf("%sConfigLoader ERROR line %d: Invalid number format (OpcodeID?). Skipping: %s - %s\n", ColoredLog.FAILURE, lineNumber, line, e.getMessage());
                    } catch (ArrayIndexOutOfBoundsException e) {
                         System.err.printf("%sConfigLoader ERROR line %d: Missing fields. Skipping: %s\n", ColoredLog.FAILURE, lineNumber, line);
                    } catch (Exception e) { 
                         System.err.printf("%sConfigLoader ERROR line %d: Cannot parse line: %s - %s\n", ColoredLog.FAILURE, lineNumber, line, e.getMessage());
                    }
                } 
                
                System.out.printf("%sInstruction configuration loaded. %d unique mnemonics, %d opcode/format definitions.\n", 
                                                            ColoredLog.SUCCESS, mnemonicMap.size(), countTotalDefinitions());

                System.out.println("--- Loaded Instruction Definitions ---");
                detailedDefinitionMap.forEach((opcodeId, formatMap) -> {
                    System.out.printf("  Opcode ID %d: ", opcodeId);
                    formatMap.forEach((format, def) -> System.out.printf("%s (%s) ", def.getMnemonic(), format));
                    System.out.println();
                });
                System.out.println("-------------------------------------");

                
                System.out.println("--- Loaded Mnemonics ---");
                mnemonicMap.forEach((mnemonic, def) -> System.out.printf("  '%s' -> %s\n", mnemonic, def.getMnemonic()));
                System.out.println("-----------------------");

                return true;
            } 
        } catch (IOException | NullPointerException e) { 
            System.err.println(ColoredLog.ERROR + "ConfigLoader FATAL ERROR: Cannot read config file resource '" + resourcePath + "': " + e.getMessage());
            e.printStackTrace(); 
            return false;
        } 
    }

    private char parseFormat(String formatStr) {
        return switch (formatStr) {
            case "R" -> 'R';
            case "I" -> 'I';
            case "D" -> 'D';
            case "B" -> 'B';
            case "CB", "C" -> 'C'; 
            case "IM", "M" -> 'M'; 
            default -> '?'; 
        };
    }

    private boolean parseFlag(String flagStr, String mnemonic, String flagName) {
        String trimmed = flagStr.trim();
        if (trimmed.equals("1")) {
            return true;
        } else if (trimmed.equals("0")) {
            return false;
        } else {
            return false;
        }
    }

    private int parseInt(String intStr, String mnemonic, String fieldName) {
        try {
            return Integer.parseInt(intStr.trim());
        } catch (NumberFormatException e) {
            System.err.printf("%sConfigLoader WARNING: Invalid integer value '%s' for %s/%s. Assuming 0.\n", ColoredLog.WARNING, intStr, mnemonic, fieldName);
            return 0; 
        }
    }

    private int parseBinary(String binStr, String mnemonic, String fieldName) {
        try {
            if (binStr.equals("N/A") || binStr.equals("IDLE")) {
                return 404; // Treat N/A or X as 0
            }
            return Integer.parseInt(binStr.trim(), 2);
        } catch (NumberFormatException e) {
            System.err.printf("%sConfigLoader WARNING: Invalid binary value '%s' for %s/%s. Assuming 0.\n", ColoredLog.WARNING, binStr, mnemonic, fieldName);
            return 0; 
        }
    }

    private int countTotalDefinitions() {
        int count = 0;
        for (Map<Character, InstructionDefinition> formatMap : detailedDefinitionMap.values()) {
            count += formatMap.size();
        }
        return count;
    }

    
    // /** Tries to determine the format based *only* on the opcode ID.
    //  *  NOTE: This can be ambiguous if R/D formats share the same ID bits.
    //  *  It might return the first format found (e.g., 'D' before 'R').
    //  *  Use getDefinition(opcodeId, format) when format is known/expected.
    // */
    // public char inferFormatFromOpcodeId(int opcodeId) {
    //     Map<Character, InstructionDefinition> formatMap = detailedDefinitionMap.get(opcodeId);
    //     if (formatMap != null && !formatMap.isEmpty()) {
    //         // Simple approach: return the first format found.
    //         // Prioritize based on typical distinctness (B, CB, IM, I are often more unique)
    //         if (formatMap.containsKey('B')) return 'B';
    //         if (formatMap.containsKey('C')) return 'C';
    //         if (formatMap.containsKey('M')) return 'M';
    //         if (formatMap.containsKey('I')) return 'I';
    //         if (formatMap.containsKey('D')) return 'D'; // Check D before R if they overlap
    //         if (formatMap.containsKey('R')) return 'R';
    //         return formatMap.keySet().iterator().next(); // Fallback: return any found format
    //     }
    //     return '?'; // Not found
    // }    
}