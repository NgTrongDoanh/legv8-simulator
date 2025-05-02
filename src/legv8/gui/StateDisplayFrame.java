package legv8.gui;

import javax.swing.*;
import javax.swing.table.*;

import legv8.util.ColoredLog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class StateDisplayFrame extends JFrame {

    protected JTable table;
    protected CustomTableModel tableModel;
    protected CustomCellRenderer cellRenderer;
    protected JScrollPane scrollPane;
    protected JPanel buttonPanel; 

    private Font font = new Font("Monospaced", Font.PLAIN, 12);
    

    public StateDisplayFrame(String title, SimulationView parent) {
        super(title);
        

        setSize(650, 450); 
        setLocationByPlatform(true); 
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); 

        initComponents();
        layoutComponents();
    }

    protected void initComponents() {
        tableModel = new CustomTableModel(); 
        table = new JTable(tableModel);
        table.setFont(font);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); 
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.getTableHeader().setReorderingAllowed(false); 

        cellRenderer = new CustomCellRenderer(); 
        table.setDefaultRenderer(Object.class, cellRenderer); 

        scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnHide = new JButton("Hide");
        btnHide.addActionListener(e -> setVisible(false));
        buttonPanel.add(btnHide);
        
    }

    protected void layoutComponents() {
        setLayout(new BorderLayout(5, 5));
        
        
        
        

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void setColumnNames(String[] names) {
        tableModel.setColumnNames(names);
    }

    public void setColumnWidths(int[] widths) {
        TableColumnModel columnModel = table.getColumnModel();
        if (widths.length != columnModel.getColumnCount()) {
            System.err.println(ColoredLog.WARNING + "Warning: Column width array length mismatch for " + getTitle());
            return;
        }
        for (int i = 0; i < widths.length; i++) {
            if (widths[i] >= 0) {
                TableColumn col = columnModel.getColumn(i);
                col.setPreferredWidth(widths[i]);
            }
        }
    }

    protected void scrollToRow(int rowIndex) {
         if (rowIndex >= 0 && rowIndex < table.getRowCount()) {
            table.setRowSelectionInterval(rowIndex, rowIndex); 
            Rectangle rect = table.getCellRect(rowIndex, 0, true); 
            if (rect != null) {
                table.scrollRectToVisible(rect); 
            }
         } else {
             table.clearSelection(); 
         }
    }

    protected static class CustomTableModel extends AbstractTableModel {
        private String[] columnNames = {};
        private Object[][] data = new Object[0][0];

        public void setColumnNames(String[] names) {
            this.columnNames = names != null ? names : new String[0];
            fireTableStructureChanged(); 
        }

        public void setData(Object[][] newData) {
            this.data = newData != null ? newData : new Object[0][0];
            fireTableDataChanged(); 
        }

        @Override public int getRowCount() { return data.length; }
        @Override public int getColumnCount() { return columnNames.length; }
        @Override public String getColumnName(int col) { return (col >= 0 && col < columnNames.length) ? columnNames[col] : null; }
        @Override public Object getValueAt(int row, int col) {
            if (row >= 0 && row < data.length && col >= 0 && col < getColumnCount()) { 
                 if (col < data[row].length) { 
                    return data[row][col];
                 }
            }
            return null;
        }
        @Override public boolean isCellEditable(int row, int col) { return false; } 
        @Override public Class<?> getColumnClass(int c) {
            
             if (getRowCount() > 0 && getValueAt(0, c) != null) {
                return getValueAt(0, c).getClass();
             }
             return String.class;
        }
    }

    protected static class CustomCellRenderer extends DefaultTableCellRenderer {
        private int highlightRow = -1; 
        private final Color highlightColor = new Color(200, 255, 200); 
        private final Color defaultBackground = UIManager.getColor("Table.background");
        private final Color alternateBackground = new Color(240, 240, 240); 

        public void setHighlightRow(int row) {
            this.highlightRow = row;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            
            if (!isSelected) {
                if (row == highlightRow) {
                    c.setBackground(highlightColor);
                } else {
                    
                     c.setBackground((row % 2 == 0) ? defaultBackground : alternateBackground);
                    
                    
                }
            }
            
            c.setForeground(UIManager.getColor("Table.foreground"));

            return c;
        }
    }
}