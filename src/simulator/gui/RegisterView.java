package simulator.gui;

import simulator.storage.RegisterStorage;
import javax.swing.*;
import java.awt.*; // Nạp Color nếu dùng

/**
 * Displays the state of the LEGv8 registers.
 */
public class RegisterView extends StateDisplayFrame {

    private RegisterStorage registerStorageRef; // Keep reference if needed later

    public RegisterView(SimulationView parent, RegisterStorage storage) {
        super("Registers (LEGv8)", parent); // Call base class constructor
        this.registerStorageRef = storage;

        setColumnNames(new String[]{"Reg #", "Name", "Hex Value (64-bit)", "Decimal Value"});
        setColumnWidths(new int[]{60, 60, 180, 180}); // Adjusted widths

        // Initial population (optional, updateData will overwrite)
        // updateData(storage, -1); // Show initial state
    }

    /**
     * Updates the table with the current register values.
     * @param storage The RegisterStorage containing the data.
     * @param lastChangedIndex The index of the register that was last written (-1 if none).
     */
    public void updateData(RegisterStorage storage, int lastChangedIndex) {
        if (storage == null) return; // Do nothing if storage is null
        this.registerStorageRef = storage; // Update reference if needed

        int numRegs = RegisterStorage.NUM_REGISTERS;
        Object[][] tableData = new Object[numRegs][4]; // 4 columns

        for (int i = 0; i < numRegs; i++) {
            long value = storage.getValue(i); // Use the passed storage object
            String regName = (i == RegisterStorage.ZERO_REGISTER_INDEX) ? "XZR" : ("X" + i);
            // Special names like SP
            if (i == 28) regName = "SP";
            // Add other special names (FP, LR) if desired
            if (i == 29) regName = "FP"; // Frame Pointer
            if (i == 30) regName = "LR"; // Link Register

            tableData[i][0] = "X" + i;
            tableData[i][1] = regName;
            tableData[i][2] = String.format("0x%016X", value); // Format as 16 hex digits
            tableData[i][3] = Long.toString(value); // Decimal value
        }

        tableModel.setData(tableData); // Update the table model
        cellRenderer.setHighlightRow(lastChangedIndex); // Set the row to highlight
        // table.repaint(); // Model change should trigger repaint, but explicit call doesn't hurt
    }
}