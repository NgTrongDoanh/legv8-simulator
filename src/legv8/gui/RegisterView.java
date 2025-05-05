/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.gui;

import legv8.storage.RegisterStorage;

/**
 * RegisterView is a GUI component that displays the register values of a LEGv8 processor.
 * It shows the register number, name, hexadecimal value, and decimal value.
 * The view can highlight the last changed register.
 */
public class RegisterView extends StateDisplayFrame {

    // --- Constructor ---
    /**
     * Constructor for RegisterView.
     * @param parent The parent SimulationView.
     * @param storage The RegisterStorage to display.
     */
    public RegisterView(SimulationView parent, RegisterStorage storage) {
        super("Registers (LEGv8)", parent); 

        setColumnNames(new String[]{"Reg #", "Name", "Hex Value (64-bit)", "Decimal Value"});
        setColumnWidths(new int[]{60, 60, 180, 180}); 
    }

    
    // --- GUI Methods ---
    /**
     * Updates the register view with the current register values.
     * @param storage The RegisterStorage containing the register values.
     * @param lastChangedIndex The index of the last changed register to highlight.
     */
    public void updateData(RegisterStorage storage, int lastChangedIndex) {
        if (storage == null) return; 

        int numRegs = RegisterStorage.NUM_REGISTERS;
        Object[][] tableData = new Object[numRegs][4]; 

        for (int i = 0; i < numRegs; i++) {
            long value = storage.getValue(i); 
            String regName = (i == RegisterStorage.ZERO_REGISTER_INDEX) ? "XZR" : ("X" + i);
            
            if (i == 28) regName = "SP";
            if (i == 29) regName = "FP"; 
            if (i == 30) regName = "LR"; 

            tableData[i][0] = "X" + i;
            tableData[i][1] = regName;
            tableData[i][2] = String.format("0x%016X", value); 
            tableData[i][3] = Long.toString(value); 
        }

        tableModel.setData(tableData); 
        cellRenderer.setHighlightRow(lastChangedIndex); 
    }
}