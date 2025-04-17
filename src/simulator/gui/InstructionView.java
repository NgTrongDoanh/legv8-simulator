package simulator.gui;

import simulator.core.InstructionMemory; // Needs InstructionMemory
import simulator.instructions.Instruction; // Needs Instruction

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.*;
import java.util.BitSet;
import java.util.List;

/**
 * Displays the contents of the Instruction Memory and highlights the current PC.
 */
public class InstructionView extends StateDisplayFrame {

    private InstructionMemory instructionMemoryRef;
    private JCheckBox chkShowHexCode; // Đổi tên cho rõ
    private JCheckBox chkShowBinaryCode; // Checkbox mới
    private int pcHighlightRow = -1;

    public InstructionView(SimulationView parent, InstructionMemory iMem) {
        super("Instruction Memory (LEGv8)", parent);
        this.instructionMemoryRef = iMem;

        // Thêm cột Binary
        setColumnNames(new String[]{"Byte Addr", "Disassembly", "Hex", "Binary"});
        setColumnWidths(new int[]{100, 250, 100, 280}); // Cần độ rộng lớn hơn cho Binary

        // Checkbox điều khiển cột Hex
        chkShowHexCode = new JCheckBox("Hex", true); // Mặc định hiện Hex
        chkShowHexCode.setToolTipText("Show/Hide Hexadecimal Bytecode Column");
        chkShowHexCode.addActionListener(e -> toggleHexColumn(chkShowHexCode.isSelected()));

        // Checkbox điều khiển cột Binary
        chkShowBinaryCode = new JCheckBox("Binary", false); // Mặc định ẩn Binary
        chkShowBinaryCode.setToolTipText("Show/Hide Binary Bytecode Column");
        chkShowBinaryCode.addActionListener(e -> toggleBinaryColumn(chkShowBinaryCode.isSelected()));

        // Add checkboxes to the button panel
        super.buttonPanel.add(chkShowHexCode, 0); // Thêm vào bên trái
        super.buttonPanel.add(chkShowBinaryCode, 1);

        toggleHexColumn(true);    // Áp dụng trạng thái ban đầu
        toggleBinaryColumn(false); // Áp dụng trạng thái ban đầu
    }

    @Override
    public void setColumnWidths(int[] widths) { // Override để xử lý đúng số cột mới
        TableColumnModel columnModel = table.getColumnModel();
        // Chỉ set nếu table đã có đủ số cột
        if (columnModel.getColumnCount() >= widths.length) {
            for (int i = 0; i < widths.length; i++) {
                 if (widths[i] >= 0) {
                     TableColumn col = columnModel.getColumn(i);
                     col.setPreferredWidth(widths[i]);
                 }
            }
        } else {
            System.err.println("Warning: Trying to set widths before table columns are fully initialized in " + getTitle());
        }
    }

    /**
     * Updates the table with instructions from InstructionMemory.
     * @param iMem The InstructionMemory containing the loaded program.
     * @param pcAddress The current value of the Program Counter (byte address).
     */
    public void updateData(InstructionMemory iMem, long pcAddress) {
         if (iMem == null) return;
         this.instructionMemoryRef = iMem;

        List<Instruction> instructions = iMem.getInstructions();
        int count = instructions.size();
        Object[][] tableData = new Object[count][4]; // 4 columns

         pcHighlightRow = -1; // Reset PC highlight row index

         for (int i = 0; i < count; i++) {
             long byteAddress = InstructionMemory.BASE_ADDRESS + (long)i * 4; // Calculate byte address
             Instruction instr = instructions.get(i);

             tableData[i][0] = String.format("0x%08X", byteAddress); // Format address
             tableData[i][1] = (instr != null) ? instr.disassemble() : "<Load Error>"; // Disassemble instruction

            // Get bytecode hex and binary representations
            if (instr != null) {
                BitSet bc = instr.getBytecode();
                // Hex
                int intRepresentation = bitSetToInt(bc); // Helper function
                tableData[i][2] = String.format("0x%08X", intRepresentation);
                // Binary
                tableData[i][3] = formatBinaryString(bc); // Helper function
            } else {
                tableData[i][2] = "N/A";
                tableData[i][3] = "N/A";
            }

            if (byteAddress == pcAddress) {
                pcHighlightRow = i;
            }
         }

         tableModel.setData(tableData);
         cellRenderer.setHighlightRow(pcHighlightRow); // Set PC row to highlight
         if (pcHighlightRow != -1) {
            scrollToRow(pcHighlightRow); // Scroll to the PC row
         }
    }

    // Helper to convert BitSet (up to 32 bits) to int
    private int bitSetToInt(BitSet bitSet) {
        int intValue = 0;
        for (int i = 0; (i < 32) && (i < bitSet.length()); i++) {
            if (bitSet.get(i)) {
                intValue |= (1 << i);
            }
        }
        return intValue;
    }

    // Helper to format BitSet as binary string with spaces
    private String formatBinaryString(BitSet bits) {
        StringBuilder sb = new StringBuilder(35); // 32 bits + 3 spaces
        for (int i = 31; i >= 0; i--) {
            sb.append(bits.get(i) ? '1' : '0');
            if (i > 0 && i % 8 == 0) {
                sb.append(' '); // Space every 8 bits
            }
        }
        return sb.toString();
    }
    
    /** Toggles the visibility of the Hex bytecode column. */
    private void toggleHexColumn(boolean show) {
        setColumnVisibility(2, show, 100); // Cột 2, độ rộng mặc định 100
    }

    /** Toggles the visibility of the Binary bytecode column. */
    private void toggleBinaryColumn(boolean show) {
        setColumnVisibility(3, show, 280); // Cột 3, độ rộng mặc định 280
    }

    /** Helper to show/hide a table column. */
    private void setColumnVisibility(int columnIndex, boolean visible, int preferredWidth) {
        if (columnIndex < 0 || columnIndex >= table.getColumnCount()) return;
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        if (visible) {
            column.setMinWidth(10); // Min width để có thể kéo lại
            column.setMaxWidth(preferredWidth * 3); // Allow some resizing
            column.setPreferredWidth(preferredWidth);
        } else {
            column.setMinWidth(0);
            column.setMaxWidth(0);
            column.setPreferredWidth(0);
        }
    }

    // Method specific to InstructionView to highlight PC row
    // Note: updateData already handles setting the highlight index
    // This could be called separately if only the PC changes without instruction reload
    public void highlightPCRow(long pcAddress) {
        if (instructionMemoryRef == null) return;
        long base = InstructionMemory.BASE_ADDRESS;
        int targetRow = -1;
        // Check if PC is within the loaded instruction range and aligned
        if (pcAddress >= base && (pcAddress - base) % 4 == 0) {
             targetRow = (int)((pcAddress - base) / 4);
             if(targetRow < 0 || targetRow >= table.getRowCount()) {
                 targetRow = -1; // PC is outside the displayed range
             }
         }
         if (pcHighlightRow != targetRow) { // Only update if the row changes
            pcHighlightRow = targetRow;
            cellRenderer.setHighlightRow(pcHighlightRow);
            if (pcHighlightRow != -1) {
                scrollToRow(pcHighlightRow);
            } else {
                table.clearSelection();
            }
            // table.repaint(); // Renderer change should trigger repaint
         }
     }
}