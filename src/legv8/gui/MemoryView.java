package legv8.gui;

import legv8.storage.MemoryStorage;
import legv8.util.ColoredLog;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MemoryView extends StateDisplayFrame {

    private MemoryStorage memoryStorageRef; 

    
    private JPanel inputPanel;
    private JLabel lblStartAddress;
    private JTextField txtStartAddress;
    private JLabel lblEndAddress;
    private JTextField txtEndAddress;
    private JButton btnUpdateView;

    
    private static final String DEFAULT_START_ADDR_STR = "0x500000"; 
    private static final String DEFAULT_END_ADDR_STR = "0x501000";   
    

    public MemoryView(SimulationView parent, MemoryStorage storage) {
        super("Data Memory (64-bit Words)", parent);
        this.memoryStorageRef = storage;

        setColumnNames(new String[]{"Byte Addr (Hex)", "Word Addr (Dec)", "Hex Value (64-bit)", "Decimal Value"});
        setColumnWidths(new int[]{130, 100, 180, 180}); 

        updateViewWithCurrentRange();
    }

    
    @Override
    protected void initComponents() {
        super.initComponents(); 

        
        inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); 

        lblStartAddress = new JLabel("Start Addr:");
        txtStartAddress = new JTextField(DEFAULT_START_ADDR_STR, 12); 
        txtStartAddress.setToolTipText("Enter start address (e.g., 0x500000 or 5242880)");

        lblEndAddress = new JLabel("End Addr:");
        txtEndAddress = new JTextField(DEFAULT_END_ADDR_STR, 12);   
        txtEndAddress.setToolTipText("Enter end address (exclusive, e.g., 0x501000 or 5246976)");

        btnUpdateView = new JButton("Update View");
        btnUpdateView.addActionListener(e -> updateViewWithCurrentRange()); 

        
        inputPanel.add(lblStartAddress);
        inputPanel.add(txtStartAddress);
        inputPanel.add(Box.createHorizontalStrut(10)); 
        inputPanel.add(lblEndAddress);
        inputPanel.add(txtEndAddress);
        inputPanel.add(Box.createHorizontalStrut(10)); 
        inputPanel.add(btnUpdateView);
    }

    
    @Override
    protected void layoutComponents() {
        setLayout(new BorderLayout(5, 5));

        
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(inputPanel, BorderLayout.NORTH);    
        southPanel.add(buttonPanel, BorderLayout.SOUTH); 

        add(scrollPane, BorderLayout.CENTER); 
        add(southPanel, BorderLayout.SOUTH);  
    }

    private void updateViewWithCurrentRange() {
        String startAddrStr = txtStartAddress.getText();
        String endAddrStr = txtEndAddress.getText();

        try {
            long startAddr = parseAddress(startAddrStr);
            long endAddr = parseAddress(endAddrStr);

            if (startAddr < 0 || endAddr < 0) {
                throw new NumberFormatException("Address cannot be negative.");
            }
            if (endAddr <= startAddr) {
                
                if (endAddr < startAddr) {
                   JOptionPane.showMessageDialog(this,
                        "End Address must be greater than Start Address.",
                        "Address Range Error", JOptionPane.WARNING_MESSAGE);
                   return; 
                } else {
                   
                   
                }

            }

            
            updateData(memoryStorageRef, startAddr, endAddr, -1L);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid address format.\nPlease use decimal (e.g., 1024) or hex (e.g., 0x400).\nError: " + ex.getMessage(),
                    "Address Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) { 
             JOptionPane.showMessageDialog(this,
                    "An error occurred while updating memory view: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
             ex.printStackTrace(); 
        }
    }

    private long parseAddress(String addrStr) throws NumberFormatException {
        if (addrStr == null) {
            throw new NumberFormatException("Address string is null");
        }
        addrStr = addrStr.trim();
        if (addrStr.isEmpty()) {
             throw new NumberFormatException("Address string is empty");
        }

        if (addrStr.startsWith("0x") || addrStr.startsWith("0X")) {
            if (addrStr.length() == 2) {
                 throw new NumberFormatException("Hex prefix '0x' requires digits following it.");
            }
            
            
            
            
            return Long.parseLong(addrStr.substring(2), 16);
        } else {
            return Long.parseLong(addrStr, 10);
        }
    }

    public void updateData(MemoryStorage storage, long startAddressByte, long endAddressByte, long lastChangedAddrByte) {
        if (storage == null) return;
        this.memoryStorageRef = storage; 

        
        Map<Long, Long> memoryContents = storage.getMemoryContentsLong(startAddressByte, endAddressByte);
        
        Map<Long, Long> sortedMemory = new TreeMap<>(memoryContents);

        List<Object[]> displayData = new ArrayList<>();
        int highlightRow = -1; 
        int displayIndex = 0;

        for (Map.Entry<Long, Long> entry : sortedMemory.entrySet()) {
            long address = entry.getKey();
            long value = entry.getValue();

            
            
            if (address >= startAddressByte && address < endAddressByte) {
                
                
                if (address >= 0 && address % 8 == 0) {
                    Object[] row = new Object[4];
                    row[0] = String.format("0x%08X", address); 
                    row[1] = address / 8; 
                    row[2] = String.format("0x%016X", value);
                    row[3] = Long.toString(value);
                    displayData.add(row);

                    
                    if (lastChangedAddrByte >= address && lastChangedAddrByte < (address + 8)) {
                        highlightRow = displayIndex;
                    }
                    displayIndex++;
                }
            }
        }

        tableModel.setData(displayData.toArray(new Object[0][])); 
        cellRenderer.setHighlightRow(highlightRow);               
        
        if (highlightRow != -1) {
             scrollToRow(highlightRow); 
        } else {
             
             if (table.getRowCount() > 0) {
                
             }
        }
    }

    
    
    public void updateData(MemoryStorage storage, long lastChangedAddrByte) {
         updateViewWithCurrentRange(); 
         
         

         long currentStart = parseAddressSilent(txtStartAddress.getText(), -1L); 
         long currentEnd = parseAddressSilent(txtEndAddress.getText(), -1L);
         if (currentStart != -1L && currentEnd != -1L && currentEnd > currentStart) {
             updateData(storage, currentStart, currentEnd, lastChangedAddrByte);
         } else {
             
             System.err.println(ColoredLog.WARNING + "MemoryView: Cannot update with invalid range in text fields during external call.");
             
             
         }
    }

    private long parseAddressSilent(String addrStr, long defaultValue) {
        try {
            return parseAddress(addrStr);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}