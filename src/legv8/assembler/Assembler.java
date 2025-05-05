/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.assembler;

import legv8.core.ProgramCounter;
import legv8.exceptions.AssemblyException;
import legv8.instructions.Instruction;
import legv8.instructions.InstructionFactory;
import legv8.util.ColoredLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A two-pass assembler for a subset of LEGv8 assembly language.
 * Pass 1 builds a symbol table mapping labels to memory addresses.
 * Pass 2 uses the symbol table and an InstructionFactory to generate
 * the corresponding Instruction objects (containing bytecode).
 */
public class Assembler {
    // --- State ---
    private final Map<String, Long> symbolTable; 
    private final List<String> processedLines; 
    private final List<String> errors;
    private final long baseAddress;

    
    // --- Constructors ---

    /**
     * Constructs an Assembler using the default base address defined in ProgramCounter.
     */
    public Assembler() {        
        this(ProgramCounter.BASE_ADDRESS);
    }

    /**
     * Constructs an Assembler configured to start assembling instructions
     * at the specified base memory address.
     * @param baseAddress The starting memory address for the first instruction.
     */
    public Assembler(long baseAddress) {
        this.baseAddress = baseAddress;
        this.symbolTable = new HashMap<>();
        this.processedLines = new ArrayList<>();
        this.errors = new ArrayList<>();
        System.out.println(ColoredLog.INFO + "Assembler initialized with base address: 0x" + Long.toHexString(baseAddress));
    }
    
    
    // --- Public API ---

    /**
     * Resets the assembler's internal state, clearing the symbol table,
     * processed lines buffer, and any recorded errors. Should be called
     * before assembling a new set of lines.
     */
    public void reset() {
        symbolTable.clear();
        processedLines.clear();
        errors.clear();
    }

    /**
     * Assembles a list of assembly code lines into a list of executable Instruction objects.
     * Executes the two passes of the assembly process.
     * @param assemblyLines A List of strings, where each string is a line of LEGv8 assembly code
     *                      (including comments and labels).
     * @return A List of assembled Instruction objects ready for loading into InstructionMemory.
     * @throws AssemblyException if any syntax errors, undefined labels, immediate range errors,
     *                           or other assembly-related issues are detected during either pass.
     *                           Call getErrors() to retrieve specific error messages.
     */
    public List<Instruction> assemble(List<String> assemblyLines) {
        reset();
        System.out.println(ColoredLog.START_PROCESS + "Assembler starting...");
       
        // Pass 1: Build Symbol Table
        System.out.println(ColoredLog.START_PROCESS + "  Starting Pass 1: Building Symbol Table...");
        try {
            buildSymbolTable(assemblyLines);
        } catch (AssemblyException e) {
            addError(0, "Pass 1 Failed: " + e.getMessage(), ""); 
            throw new AssemblyException(aggregateErrors());
        }
        System.out.println(ColoredLog.SUCCESS + "  Pass 1 finished. Symbol Table:");
        symbolTable.forEach((label, address) -> System.out.printf(ColoredLog.INFO + "    %-20s : 0x%X\n", label, address));

        // Pass 2: Generate Instructions
        System.out.println(ColoredLog.START_PROCESS + "  Starting Pass 2: Generating Instructions...");
        List<Instruction> assembledInstructions = generateInstructions();

        // Check for errors after Pass 2
        if (!errors.isEmpty()) {
            System.err.println("  Pass 2 completed with errors.");
            throw new AssemblyException(aggregateErrors());
        }

        System.out.println(ColoredLog.SUCCESS + "  Pass 2 finished. Generated " + assembledInstructions.size() + " instructions.");
        System.out.println(ColoredLog.END_PROCESS + "Assembler finished successfully.");
        return assembledInstructions;
    }

    /**
     * Retrieves a list of error messages encountered during the assembly process.
     * @return A List of strings, each representing an error message.
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors); 
    }
    
    
    // --- Pass 1: Symbol Table Construction ---

    /**
     * Builds the symbol table from the provided assembly lines.
     * Each label is mapped to its corresponding address in memory.
     * @param rawLines A List of strings, where each string is a line of LEGv8 assembly code
     *                 (including comments and labels).
     * @throws AssemblyException if any syntax errors or duplicate labels are detected.
     */
    private void buildSymbolTable(List<String> rawLines) {
        long currentAddress = this.baseAddress;
        int lineNumber = 0;

        for (String line : rawLines) {
            lineNumber++;
            String processed = preprocessLine(line);
            
            if (processed.isEmpty()) continue;

            if (processed.endsWith(":")) { 
                String label = processed.substring(0, processed.length() - 1).trim();
                
                if (!isValidLabel(label)) throw new AssemblyException(formatError(lineNumber, "Invalid label name: '" + label + "'", line));
                
                if (symbolTable.containsKey(label)) throw new AssemblyException(formatError(lineNumber, "Duplicate label definition: '" + label + "'", line));

                symbolTable.put(label, currentAddress);
            } else {
                processedLines.add(processed);
                currentAddress += 4; 
            }
        }
    }


    // --- Pass 2: Instruction Generation ---

    /**
     * Performs the second pass of assembly. Iterates through the preprocessed lines
     * (stored during Pass 1), uses the InstructionFactory and the symbol table
     * to generate the bytecode (Instruction object) for each line.
     * Records errors encountered during instruction creation.
     * @return A list of generated Instruction objects.
     */
    private List<Instruction> generateInstructions() {
        List<Instruction> instructions = new ArrayList<>();
        long currentAddress = this.baseAddress;
        int processedLineNumber = 0; 

        for (String line : processedLines) {
            processedLineNumber++;
            
            try {    
                Instruction instruction = InstructionFactory.createFromAssembly(line, symbolTable, currentAddress);
                instructions.add(instruction);
            } catch (AssemblyException | IllegalArgumentException | IllegalStateException e) {    
                addError(processedLineNumber, e.getMessage(), line);
            } catch (Exception e) {
                addError(processedLineNumber, "Unexpected error: " + e.getMessage(), line);
                e.printStackTrace(); 
            } finally {
                currentAddress += 4; 
            }
        }
        return instructions;
    }


    // --- Helper Methods ---

    /**
     * Preprocesses a line of assembly code by removing comments and trimming whitespace.
     * @param line The raw line of assembly code.
     * @return The preprocessed line with comments removed and leading/trailing whitespace trimmed.
     */
    private String preprocessLine(String line) {
        int commentIndex = line.indexOf("//");
        if (commentIndex != -1) line = line.substring(0, commentIndex);
        return line.trim();
    }

    /**
     * Checks if a given string is a valid label name according to simple rules
     * (starts with letter or underscore, contains letters, digits, or underscores).
     * @param label The potential label name to validate.
     * @return true if the label is valid, false otherwise.
     */
    private boolean isValidLabel(String label) {
        if (label == null || label.isEmpty()) return false;
        
        if (!Character.isLetter(label.charAt(0)) && label.charAt(0) != '_') return false;
        
        for (int i = 1; i < label.length(); i++) {
            char c = label.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') return false;
        }

        return true;
    }
    
    /**
     * Formats an error message including the approximate line number and the code content.
     * @param lineNumber The approximate line number where the error occurred (relative to raw input or processed lines).
     * @param message The specific error description.
     * @param lineContent The content of the line causing the error.
     * @return A formatted error string.
     */
    private String formatError(int lineNumber, String message, String lineContent) {
        return String.format("AsmError (Line ~%d): %s [Code: '%s']", lineNumber, message, lineContent);
    }

    /**
     * Adds a formatted error message to the internal error list and prints it to System.err.
     * @param lineNumber The approximate line number.
     * @param message The error message.
     * @param lineContent The relevant line content.
     */
    private void addError(int lineNumber, String message, String lineContent) {
        String formattedError = formatError(lineNumber, message, lineContent);
        errors.add(formattedError);
        System.err.println(ColoredLog.ERROR + formattedError); 
    }

    /**
     * Aggregates all recorded errors into a single multi-line string for reporting.
     * @return A string detailing all assembly errors, or a success message if no errors occurred.
     */
    private String aggregateErrors() {
        if (errors.isEmpty()) return "Assembly successful.";
       
        StringBuilder sb = new StringBuilder(ColoredLog.FAILURE + "Assembly failed with " + errors.size() + " error(s):\n");
        for (String err : errors) {
            sb.append(ColoredLog.INFO + "  - ").append(err).append("\n");
        }
        
        return sb.toString();
    }
}