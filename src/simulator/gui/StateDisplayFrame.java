package simulator.gui;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Base class for windows displaying simulator state in a table (Registers, Memory, Instructions).
 */
public abstract class StateDisplayFrame extends JFrame {

    protected JTable table;
    protected CustomTableModel tableModel;
    protected CustomCellRenderer cellRenderer;
    protected JScrollPane scrollPane;
    protected JPanel buttonPanel; // Panel at the bottom for buttons
    // Keep reference to parent SimulationView if needed for coordination, but not strictly necessary for display
    // protected SimulationView parentView;

    /**
     * Constructor for the state display frame.
     * @param title Window title.
     * @param parent The parent SimulationView (can be null if not needed).
     */
    public StateDisplayFrame(String title, SimulationView parent) {
        super(title);
        // this.parentView = parent;

        setSize(550, 450); // Default size, can be adjusted
        setLocationByPlatform(true); // Let OS decide position initially
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); // Hide instead of exiting app

        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        tableModel = new CustomTableModel(); // Create the table model
        table = new JTable(tableModel);
        table.setFont(new Font("Monospaced", Font.PLAIN, 12));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Allow horizontal scrolling
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.getTableHeader().setReorderingAllowed(false); // Prevent column reordering

        cellRenderer = new CustomCellRenderer(); // Custom renderer for highlighting
        table.setDefaultRenderer(Object.class, cellRenderer); // Apply renderer to all columns

        scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Bottom panel for buttons
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnHide = new JButton("Hide");
        btnHide.addActionListener(e -> setVisible(false));
        buttonPanel.add(btnHide);
        // Add other controls like "Format Toggle" or "Hide Zeros" here later if needed
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(5, 5));
        // Optional Title Label at the top
        // JLabel lblTitle = new JLabel(getTitle(), SwingConstants.CENTER);
        // lblTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        // add(lblTitle, BorderLayout.NORTH);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /** Sets the column names for the table. */
    public void setColumnNames(String[] names) {
        tableModel.setColumnNames(names);
    }

    /** Sets the preferred widths for the table columns. */
    public void setColumnWidths(int[] widths) {
        TableColumnModel columnModel = table.getColumnModel();
        if (widths.length != columnModel.getColumnCount()) {
            System.err.println("Warning: Column width array length mismatch for " + getTitle());
            return;
        }
        for (int i = 0; i < widths.length; i++) {
            if (widths[i] >= 0) {
                TableColumn col = columnModel.getColumn(i);
                col.setPreferredWidth(widths[i]);
            }
        }
    }

    /** Scrolls the table view to make the specified row visible. */
    protected void scrollToRow(int rowIndex) {
         if (rowIndex >= 0 && rowIndex < table.getRowCount()) {
            table.setRowSelectionInterval(rowIndex, rowIndex); // Select the row
            Rectangle rect = table.getCellRect(rowIndex, 0, true); // Get cell bounds
            if (rect != null) {
                table.scrollRectToVisible(rect); // Scroll viewport to the cell
            }
         } else {
             table.clearSelection(); // Clear selection if row index is invalid
         }
    }

    // --- Inner Classes for Table Model and Renderer ---

    /** Custom Table Model to hold state data. */
    protected static class CustomTableModel extends AbstractTableModel {
        private String[] columnNames = {};
        private Object[][] data = new Object[0][0];

        // Constructor có thể cần khởi tạo số cột ban đầu
        public CustomTableModel() {
            this(new String[0], new Object[0][0]); // Default constructor
        }

        public CustomTableModel(String[] columnNames, Object[][] data) {
            this.columnNames = columnNames != null ? columnNames : new String[0];
            this.data = data != null ? data : new Object[0][0];
        }

        public void setColumnNames(String[] names) {
            this.columnNames = names != null ? names : new String[0];
            fireTableStructureChanged(); // Notify table structure changed
        }

        public void setData(Object[][] newData) {
            this.data = newData != null ? newData : new Object[0][0];
            // Quan trọng: Kiểm tra xem số cột dữ liệu mới có khớp không?
            // Hoặc đảm bảo dữ liệu luôn đúng trước khi gọi setData.
            fireTableDataChanged(); // Notify table data changed
        }

        @Override public int getRowCount() { return data.length; }
        @Override public int getColumnCount() { return columnNames.length; }
        @Override public String getColumnName(int col) { return (col >= 0 && col < columnNames.length) ? columnNames[col] : null; }
        @Override public Object getValueAt(int row, int col) {
            if (row >= 0 && row < data.length && col >= 0 && col < getColumnCount()) { // Use getColumnCount()
                 if (col < data[row].length) { // Check if column exists in this row's data
                    return data[row][col];
                 }
            }
            return null;
        }
        @Override public boolean isCellEditable(int row, int col) { return false; } // Not editable
        @Override public Class<?> getColumnClass(int c) {
            // Determine column class from first row if possible, default to String
             if (getRowCount() > 0 && getValueAt(0, c) != null) {
                return getValueAt(0, c).getClass();
             }
             return String.class;
        }
    }

    /** Custom Cell Renderer for highlighting changes or specific rows. */
    protected static class CustomCellRenderer extends DefaultTableCellRenderer {
        private int highlightRow = -1; // Row to highlight (e.g., changed register, PC)
        private final Color highlightColor = new Color(200, 255, 200); // Light green highlight
        private final Color defaultBackground = UIManager.getColor("Table.background");
        private final Color alternateBackground = new Color(240, 240, 240); // Light gray for alternate rows

        /** Sets the row index to be highlighted. Use -1 to clear highlight. */
        public void setHighlightRow(int row) {
            this.highlightRow = row;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // Let the default renderer handle basic setup (text, alignment, selection color)
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Apply custom background colors if not selected
            if (!isSelected) {
                if (row == highlightRow) {
                    c.setBackground(highlightColor);
                } else {
                    // Optional: Alternate row coloring for readability
                     c.setBackground((row % 2 == 0) ? defaultBackground : alternateBackground);
                    // Or just use default:
                    // c.setBackground(defaultBackground);
                }
            }
            // Keep default foreground color or customize if needed
            c.setForeground(UIManager.getColor("Table.foreground"));

            return c;
        }
    }
}