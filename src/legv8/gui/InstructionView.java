/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.gui;

import legv8.core.InstructionMemory; 
import legv8.core.ProgramCounter;
import legv8.instructions.Instruction;
import legv8.util.ColoredLog;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.util.BitSet;
import java.util.List;

/**
 * InstructionView is a GUI component that displays the instruction memory of a LEGv8 processor.
 * It shows the byte address, disassembly, and bytecode in both hexadecimal and binary formats.
 * The view can highlight the current program counter (PC) address.
 */
public class InstructionView extends StateDisplayFrame {
    // GUI components
    private JCheckBox chkShowHexCode; 
    private JCheckBox chkShowBinaryCode; 
    
    // Data references
    private InstructionMemory instructionMemoryRef;
    private int pcHighlightRow = -1; 


    // --- Constructor ---
    
    /**
     * Constructor for InstructionView.
     * @param parent The parent SimulationView.
     * @param iMem The InstructionMemory to display.
     */
    public InstructionView(SimulationView parent, InstructionMemory iMem) {
        super("Instruction Memory (LEGv8)", parent);
        this.instructionMemoryRef = iMem;

        setColumnNames(new String[]{"Byte Addr", "Disassembly", "Bytecode (Hex)", "Bytecode (Bin)"});
        setColumnWidths(new int[]{100, 250, 150, 280}); 
        
        chkShowHexCode = new JCheckBox("Hex", true); 
        chkShowHexCode.setToolTipText("Show/Hide Hexadecimal Bytecode Column");
        chkShowHexCode.addActionListener(e -> toggleHexColumn(chkShowHexCode.isSelected()));
        
        chkShowBinaryCode = new JCheckBox("Binary", false); 
        chkShowBinaryCode.setToolTipText("Show/Hide Binary Bytecode Column");
        chkShowBinaryCode.addActionListener(e -> toggleBinaryColumn(chkShowBinaryCode.isSelected()));
        
        super.buttonPanel.add(chkShowHexCode, 0); 
        super.buttonPanel.add(chkShowBinaryCode, 1);

        toggleHexColumn(true);    
        toggleBinaryColumn(false); 
    }

    
    // --- GUI Methods ---

    /**
     * Set the column widths for the table.
     * @param widths An array of integers representing the desired widths for each column.
     */
    @Override
    public void setColumnWidths(int[] widths) { 
        TableColumnModel columnModel = table.getColumnModel();
        
        if (columnModel.getColumnCount() >= widths.length) {
            for (int i = 0; i < widths.length; i++) {
                if (widths[i] >= 0) {
                    TableColumn col = columnModel.getColumn(i);
                    col.setPreferredWidth(widths[i]);
                }
            }
        } else {
            System.err.println(ColoredLog.WARNING + "Warning: Trying to set widths before table columns are fully initialized in " + getTitle());
        }
    }
    
    /**
     * Toggle the visibility of the hexadecimal bytecode column.
     * @param show True to show the column, false to hide it.
     */
    private void toggleHexColumn(boolean show) {
        setColumnVisibility(2, show, 100); 
    }

    /**
     * Toggle the visibility of the binary bytecode column.
     * @param show True to show the column, false to hide it.
     */
    private void toggleBinaryColumn(boolean show) {
        setColumnVisibility(3, show, 280); 
    }

    /**
     * Set the visibility of a specific column in the table.
     * @param columnIndex The index of the column to modify.
     * @param visible True to show the column, false to hide it.
     * @param preferredWidth The preferred width of the column when visible.
     */
    private void setColumnVisibility(int columnIndex, boolean visible, int preferredWidth) {
        if (columnIndex < 0 || columnIndex >= table.getColumnCount()) return;
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        if (visible) {
            column.setMinWidth(10); 
            column.setMaxWidth(preferredWidth * 3); 
            column.setPreferredWidth(preferredWidth);
        } else {
            column.setMinWidth(0);
            column.setMaxWidth(0);
            column.setPreferredWidth(0);
        }
    }

    /**
     * Update the instruction memory display.
     * This method retrieves the current instruction memory from the reference and updates the table.
     * @param pcAddress The current program counter address.
     */
    public void highlightPCRow(long pcAddress) {
        if (instructionMemoryRef == null) return;
        long base = ProgramCounter.BASE_ADDRESS;
        int targetRow = -1;
        
        if (pcAddress >= base && (pcAddress - base) % 4 == 0) {
            targetRow = (int)((pcAddress - base) / 4);
            if(targetRow < 0 || targetRow >= table.getRowCount()) {
                targetRow = -1; 
            }
        }

        if (pcHighlightRow != targetRow) {
            pcHighlightRow = targetRow;
            cellRenderer.setHighlightRow(pcHighlightRow);
            if (pcHighlightRow != -1) {
                scrollToRow(pcHighlightRow);
            } else {
                table.clearSelection();
            }
            table.repaint(); 
        }
    }

    // --- Data Processing Methods ---

    /**
     * Update the data displayed in the instruction memory table.
     * This method retrieves the instructions from the InstructionMemory and populates the table.
     * It also highlights the row corresponding to the current program counter (PC) address.
     * @param iMem
     * @param pcAddress
     */
    public void updateData(InstructionMemory iMem, long pcAddress) {
        if (iMem == null) return;
        this.instructionMemoryRef = iMem;
        List<Instruction> instructions = instructionMemoryRef.getInstructions();

        int count = instructions.size();
        Object[][] tableData = new Object[count][4]; 

        pcHighlightRow = -1; 
        for (int i = 0; i < count; i++) {
            long byteAddress = ProgramCounter.BASE_ADDRESS + (long)i * 4; 
            Instruction instr = instructions.get(i);

            tableData[i][0] = String.format("0x%08X", byteAddress); 
            tableData[i][1] = (instr != null) ? instr.disassemble() : "<Load Error>"; 
            
            if (instr != null) {
                BitSet bc = instr.getBytecode();
                
                int intRepresentation = bitSetToInt(bc); 
                tableData[i][2] = String.format("0x%08X", intRepresentation);    
                tableData[i][3] = formatBinaryString(bc); 
            } else {
                tableData[i][2] = "N/A";
                tableData[i][3] = "N/A";
            }

            if (byteAddress == pcAddress) {
                pcHighlightRow = i;
            }
        }

        tableModel.setData(tableData);
        cellRenderer.setHighlightRow(pcHighlightRow); 
        if (pcHighlightRow != -1) {
            scrollToRow(pcHighlightRow); 
        }
    }

    /**
     * Convert a BitSet to an integer
     * @param bitSet
     * @return Value of the BitSet as an integer
     */    
    private int bitSetToInt(BitSet bitSet) {
        int intValue = 0;
        for (int i = 0; (i < 32) && (i < bitSet.length()); i++) {
            if (bitSet.get(i)) {
                intValue |= (1 << i);
            }
        }
        return intValue;
    }
    
    /**
     * Format a BitSet as a binary string
     * This method is used to display the bytecode in binary format, separated by spaces every 8 bits for readability.
     * @param bits
     * @return Formatted binary string
     */
    private String formatBinaryString(BitSet bits) {
        StringBuilder sb = new StringBuilder(35); 
        for (int i = 31; i >= 0; i--) {
            sb.append(bits.get(i) ? '1' : '0');
            if (i > 0 && i % 8 == 0) {
                sb.append(' '); 
            }
        }
        return sb.toString();
    }
}