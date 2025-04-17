package simulator.assembler;

import simulator.exceptions.AssemblyException;
import simulator.instructions.Instruction;
import simulator.instructions.InstructionFactory; // Crucial dependency
import simulator.core.ProgramCounter; // To get BASE_ADDRESS

import java.util.*;

/**
 * A simple two-pass assembler for LEGv8 assembly code.
 * Handles labels and generates a list of Instruction objects.
 */
public class Assembler {

    private final Map<String, Long> symbolTable; // Label -> Byte Address
    private final List<String> processedLines; // Assembly lines after Pass 1 (no labels/comments)
    private final List<String> errors;
    private final long baseAddress;

    public Assembler() {
        // Default base address, consider making it configurable
        this(ProgramCounter.BASE_ADDRESS);
    }

    public Assembler(long baseAddress) {
        this.baseAddress = baseAddress;
        this.symbolTable = new HashMap<>();
        this.processedLines = new ArrayList<>();
        this.errors = new ArrayList<>();
        System.out.println("Assembler initialized with base address: 0x" + Long.toHexString(baseAddress));
    }

    /**
     * Assembles the given list of assembly code lines.
     *
     * @param assemblyLines Raw lines of assembly code.
     * @return A list of assembled Instruction objects.
     * @throws AssemblyException if errors occur during assembly. The exception message
     *                           may contain aggregated error details. Use getErrors() for individual errors.
     */
    public List<Instruction> assemble(List<String> assemblyLines) {
        reset();
        System.out.println("Assembler starting...");

        // --- Pass 1: Build Symbol Table & Process Lines ---
        System.out.println("  Starting Pass 1: Building Symbol Table...");
        try {
            buildSymbolTable(assemblyLines);
        } catch (AssemblyException e) {
            // Pass 1 errors are usually fatal for Pass 2
            addError(0, "Pass 1 Failed: " + e.getMessage(), ""); // Add context
            throw new AssemblyException(aggregateErrors());
        }
        System.out.println("  Pass 1 finished. Symbol Table:");
        symbolTable.forEach((label, address) -> System.out.printf("    %-15s : 0x%X\n", label, address));

        // --- Pass 2: Generate Machine Code (Instructions) ---
        System.out.println("  Starting Pass 2: Generating Instructions...");
        List<Instruction> assembledInstructions = generateInstructions();

        if (!errors.isEmpty()) {
            System.err.println("  Pass 2 completed with errors.");
            throw new AssemblyException(aggregateErrors());
        }

        System.out.println("  Pass 2 finished. Generated " + assembledInstructions.size() + " instructions.");
        System.out.println("Assembler finished successfully.");
        return assembledInstructions;
    }

    /** Resets the assembler state for a new assembly process. */
    public void reset() {
        symbolTable.clear();
        processedLines.clear();
        errors.clear();
    }

    /** Returns the list of errors encountered during the last assembly attempt. */
    public List<String> getErrors() {
        return new ArrayList<>(errors); // Return a copy
    }

    // --- Pass 1 Implementation ---
    private void buildSymbolTable(List<String> rawLines) {
        long currentAddress = this.baseAddress;
        int lineNumber = 0;

        for (String line : rawLines) {
            lineNumber++;
            String processed = preprocessLine(line);
            if (processed.isEmpty()) continue;

            if (processed.endsWith(":")) { // Found a label definition
                String label = processed.substring(0, processed.length() - 1).trim();
                if (!isValidLabel(label)) {
                    // Invalid label name is a critical error for Pass 1
                    throw new AssemblyException(formatError(lineNumber, "Invalid label name: '" + label + "'", line));
                }
                if (symbolTable.containsKey(label)) {
                    // Duplicate label definition is critical
                    throw new AssemblyException(formatError(lineNumber, "Duplicate label definition: '" + label + "'", line));
                }
                symbolTable.put(label, currentAddress);
                // Labels don't advance the address or add to processedLines for Pass 2
            } else {
                // This is likely an instruction line
                processedLines.add(processed);
                currentAddress += 4; // Assume each instruction is 4 bytes
            }
        }
    }

    // --- Pass 2 Implementation ---
    private List<Instruction> generateInstructions() {
        List<Instruction> instructions = new ArrayList<>();
        long currentAddress = this.baseAddress;
        int processedLineNumber = 0; // Track line number within processedLines

        for (String line : processedLines) {
            processedLineNumber++;
            try {
                // Use the initialized InstructionFactory to create the instruction object
                Instruction instruction = InstructionFactory.createFromAssembly(line, symbolTable, currentAddress);
                instructions.add(instruction);
            } catch (AssemblyException | IllegalArgumentException | IllegalStateException e) {
                // Catch errors from the factory (parsing, undefined labels, range errors, etc.)
                // Also catch IllegalArgumentException/IllegalStateException for robustness
                addError(processedLineNumber, e.getMessage(), line);
                 // Continue assembling other lines if possible, errors aggregated at the end
            } catch (Exception e) {
                // Catch unexpected runtime errors during assembly of this line
                addError(processedLineNumber, "Unexpected error: " + e.getMessage(), line);
                e.printStackTrace(); // Log unexpected errors fully
            } finally {
                 currentAddress += 4; // Always advance address, even if line had error
            }
        }
        return instructions;
    }

    // --- Helper Methods ---

    private String preprocessLine(String line) {
        int commentIndex = line.indexOf("//");
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex);
        }
        // Add handling for other comment types if needed (e.g., ';', '#')
        return line.trim();
    }

    private boolean isValidLabel(String label) {
        if (label == null || label.isEmpty()) return false;
        // Must start with a letter or underscore
        if (!Character.isLetter(label.charAt(0)) && label.charAt(0) != '_') return false;
        // Rest can be letters, digits, or underscores
        for (int i = 1; i < label.length(); i++) {
            char c = label.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') return false;
        }
        // Optional: Check against reserved keywords/mnemonics if InstructionFactory is accessible here
        // if (InstructionFactory.isMnemonic(label)) return false; // Needs static method in Factory
        return true;
    }

    private String formatError(int lineNumber, String message, String lineContent) {
        return String.format("AsmError (Line ~%d): %s [Code: '%s']", lineNumber, message, lineContent);
    }

    private void addError(int lineNumber, String message, String lineContent) {
        String formattedError = formatError(lineNumber, message, lineContent);
        errors.add(formattedError);
        System.err.println(formattedError); // Print error immediately
    }

    private String aggregateErrors() {
        if (errors.isEmpty()) return "Assembly successful.";
        StringBuilder sb = new StringBuilder("Assembly failed with " + errors.size() + " error(s):\n");
        for (String err : errors) {
            sb.append("  - ").append(err).append("\n");
        }
        return sb.toString();
    }
}