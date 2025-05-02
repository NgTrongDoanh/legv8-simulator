package legv8.gui;

import legv8.storage.RegisterStorage;
import javax.swing.*;
import java.awt.*; 

public class RegisterView extends StateDisplayFrame {

    private RegisterStorage registerStorageRef; 

    public RegisterView(SimulationView parent, RegisterStorage storage) {
        super("Registers (LEGv8)", parent); 
        this.registerStorageRef = storage;

        setColumnNames(new String[]{"Reg #", "Name", "Hex Value (64-bit)", "Decimal Value"});
        setColumnWidths(new int[]{60, 60, 180, 180}); 

        
        
    }

    public void updateData(RegisterStorage storage, int lastChangedIndex) {
        if (storage == null) return; 
        this.registerStorageRef = storage; 

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