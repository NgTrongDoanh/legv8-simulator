package simulator.gui;

import simulator.storage.MemoryStorage;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Displays the state of the Data Memory.
 */
public class MemoryView extends StateDisplayFrame {

    private MemoryStorage memoryStorageRef; // Keep reference if needed

    public MemoryView(SimulationView parent, MemoryStorage storage) {
        super("Data Memory (64-bit Words)", parent);
        this.memoryStorageRef = storage;

        setColumnNames(new String[]{"Byte Addr (Hex)", "Word Addr (Dec)", "Hex Value (64-bit)", "Decimal Value"});
        setColumnWidths(new int[]{130, 100, 180, 180}); // Adjusted widths

        // updateData(storage, -1L); // Show initial state
    }

    /**
     * Updates the table with the current memory contents.
     * Only shows word-aligned addresses that have been written to.
     * @param storage The MemoryStorage containing the data.
     * @param lastChangedAddrByte The byte address that was last accessed (-1 if none).
     */
    public void updateData(MemoryStorage storage, long lastChangedAddrByte) {
        if (storage == null) return;
        this.memoryStorageRef = storage;

        Map<Long, Long> memoryContents = storage.getMemoryContents();
        // Sort by address for consistent display
        Map<Long, Long> sortedMemory = new TreeMap<>(memoryContents);

        List<Object[]> displayData = new ArrayList<>();
        int highlightRow = -1; // Index in the *display* list
        int displayIndex = 0;

        for (Map.Entry<Long, Long> entry : sortedMemory.entrySet()) {
            long address = entry.getKey();
            long value = entry.getValue();

            // Only display addresses aligned to 8 bytes (64-bit words)
            // And optionally filter non-zero values if memory becomes large
            if (address >= 0 && address % 8 == 0 /* && value != 0L */) {
                Object[] row = new Object[4];
                row[0] = String.format("0x%08X", address); // Show 8 hex digits for address
                row[1] = address / 8; // Word address
                row[2] = String.format("0x%016X", value);
                row[3] = Long.toString(value);
                displayData.add(row);

                // Check if the last changed address falls within this displayed word
                if (lastChangedAddrByte >= address && lastChangedAddrByte < (address + 8)) {
                    highlightRow = displayIndex;
                }
                displayIndex++;
            }
        }

        tableModel.setData(displayData.toArray(new Object[0][])); // Update model
        cellRenderer.setHighlightRow(highlightRow);               // Set highlight
        // Ensure highlighted row is visible if valid
        if (highlightRow != -1) {
             scrollToRow(highlightRow); // Scroll to make the highlighted row visible
        }
    }
}