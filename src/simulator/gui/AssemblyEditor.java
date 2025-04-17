package simulator.gui;

// Đảm bảo các import là chính xác (javax.swing.*, java.awt.*, java.io.*, etc.)
// Quan trọng: Sửa đổi phương thức `assembleCode()` để gọi `mainApp.assembleCode()`:

import simulator.assembler.Assembler; // Cần Assembler
import simulator.core.InstructionMemory; // Cần BASE_ADDRESS

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.BitSet; // Cần nếu trả về BitSet, nhưng giờ nên trả về Instruction

public class AssemblyEditor extends JFrame implements ActionListener {
    
    private Legv8SimApp mainApp; // Tham chiếu App chính
    private JTextArea textArea;
    private JScrollPane scrollPane;
    private JButton btnAssemble;
    private JButton btnClose; // Will be "Hide"
    private JFileChooser fileChooser;
    private File currentFile;
    private JMenuItem miNew, miOpen, miSave, miSaveAs, miExitEditor;

    private String originalContent = "";
    private boolean hasUnsavedChanges = false;

    public AssemblyEditor(Legv8SimApp app) {
        this.mainApp = app;
        setTitle("LEGv8 Assembly Editor");
        setSize(700, 500);
        setLocationRelativeTo(app);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Handle close via listener

        initComponents();
        layoutComponents();
        updateTitle();

        // Listener for unsaved changes
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
           @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { markChanged(); }
           @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { markChanged(); }
           @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { /* Style change */ }
       });

        // Listener for window closing
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { handleClose(); }
        });
    }

    private void initComponents() {
        textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setTabSize(4);
        textArea.setMargin(new Insets(5, 5, 5, 5));
        scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        btnAssemble = new JButton("Assemble"); // Chỉ Assemble, không Load trực tiếp
        btnClose = new JButton("Hide");

        btnAssemble.setToolTipText("Assemble the current code");
        btnClose.setToolTipText("Hide this editor window");

        btnAssemble.addActionListener(this);
        btnClose.addActionListener(this);

        fileChooser = new JFileChooser("."); // Start in current directory
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Assembly Files (*.s, *.asm, *.txt)", "s", "asm", "txt");
        fileChooser.setFileFilter(filter);

        currentFile = null;
        originalContent = "";
        hasUnsavedChanges = false;
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu fm = new JMenu("File");

        miNew = new JMenuItem("New");
        miOpen = new JMenuItem("Open...");
        miSave = new JMenuItem("Save");
        miSaveAs = new JMenuItem("Save As...");
        miExitEditor = new JMenuItem("Hide Window"); // Renamed from Exit

        miNew.addActionListener(this);
        miOpen.addActionListener(this);
        miSave.addActionListener(this);
        miSaveAs.addActionListener(this);
        miExitEditor.addActionListener(this);

        // Accelerators (adjust for platform if needed)
        miNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        miOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        miSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        miExitEditor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx())); // Cmd+W or Ctrl+W

        fm.add(miNew);
        fm.add(miOpen);
        fm.add(miSave);
        fm.add(miSaveAs);
        fm.addSeparator();
        fm.add(miExitEditor);
        mb.add(fm);
        return mb;
    }

    private void layoutComponents() {
         setLayout(new BorderLayout(5, 5));
         add(scrollPane, BorderLayout.CENTER);
         JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
         bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5));
         bottomPanel.add(btnAssemble);
         bottomPanel.add(btnClose);
         add(bottomPanel, BorderLayout.SOUTH);
         setJMenuBar(createMenuBar());
     }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnAssemble) {
            assembleCurrentCode(); // Call the assemble method
        } else if (src == btnClose || src == miExitEditor) {
            handleClose();
        } else if (src == miNew) {
            newFile();
        } else if (src == miOpen) {
            openFile();
        } else if (src == miSave) {
            saveFile();
        } else if (src == miSaveAs) {
            saveFileAs();
        }
    }

    // --- Core Actions ---

    /** Assembles the code currently in the text area. */
    private void assembleCurrentCode() {
        System.out.println("Assembly Editor: Assemble requested.");
        String code = textArea.getText();
        List<String> codeLines = Arrays.asList(code.split("\\r?\\n")); // Split into lines

        // Prompt to save unsaved changes before assembling (optional but good practice)
        // boolean proceed = true;
        // if (hasUnsavedChanges) {
        //     int choice = JOptionPane.showConfirmDialog(this,
        //         "There are unsaved changes. Save before assembling?",
        //         "Unsaved Changes",
        //         JOptionPane.YES_NO_CANCEL_OPTION,
        //         JOptionPane.WARNING_MESSAGE);

        //     if (choice == JOptionPane.YES_OPTION) {
        //         proceed = saveFile(); // Attempt to save, proceed only if successful
        //     } else if (choice == JOptionPane.CANCEL_OPTION) {
        //         proceed = false; // User cancelled the operation
        //     }
        //     // If NO_OPTION, proceed without saving
        // }

        // if (proceed && mainApp != null) {
        //     // *** Call the main application's assemble method ***
        //     String sourceName = (currentFile != null) ? currentFile.getName() : "Editor Content";
        //     mainApp.assembleCode(codeLines, sourceName);
        //     // The mainApp will handle success/failure messages and updating state
        // } else if (!proceed) {
        //      System.out.println("Assembly cancelled or save failed.");
        // }

        if (mainApp != null) {
            // Xác định nguồn code (tên file hoặc "Editor")
            String sourceName = (currentFile != null) ? currentFile.getName() : "Editor Content";
            // *** Gọi hàm assemble của App chính ***
            mainApp.assembleCode(codeLines, sourceName);
            // App chính sẽ xử lý kết quả và cập nhật trạng thái (assemblyReady)
            // Có thể đóng editor sau khi assemble thành công? (Tùy chọn)
            // if (mainApp.isAssemblyReady()) { // Cần thêm getter isAssemblyReady() vào mainApp
            //    setVisible(false);
            // }
        }
    }

    /** Sets the content of the text area. */
    public void setSourceCode(String content) {
        textArea.setText(content != null ? content : "");
        textArea.setCaretPosition(0); // Đưa con trỏ về đầu
        // Có thể cần reset cờ unsaved changes ở đây
        markChangesSaved();
    }

    private void handleClose() {
        if (promptToSave()) {
            setVisible(false); // Just hide the window
        }
    }

    // --- File Handling Methods (Similar to previous version) ---

    private void newFile() {
        if (promptToSave()) {
            textArea.setText("");
            currentFile = null;
            originalContent = "";
            markChangesSaved(); // Update title and flag
            textArea.setCaretPosition(0); // Move cursor to start
        }
    }

    private void openFile() {
        if (promptToSave()) {
            int returnVal = fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                readFile(fileChooser.getSelectedFile());
            }
        }
    }

    private void readFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            textArea.read(reader, null); // Efficiently reads file content into text area
            currentFile = file;
            originalContent = textArea.getText();
            markChangesSaved();
            textArea.setCaretPosition(0); // Move cursor to start
            System.out.println("Opened file: " + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + file.getName() + "\n" + e.getMessage(), "File Read Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean saveFile() {
        if (currentFile == null) {
            return saveFileAs(); // If no current file, use Save As logic
        } else {
            return performSave(currentFile);
        }
    }

    private boolean saveFileAs() {
        if (currentFile != null) {
             fileChooser.setSelectedFile(currentFile); // Suggest current filename
        } else {
             fileChooser.setSelectedFile(new File("untitled.s")); // Default filename
        }

        int returnVal = fileChooser.showSaveDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return false; // User cancelled
        }

        File fileToSave = fileChooser.getSelectedFile();
        // Ensure file has a proper extension (optional)
        String path = fileToSave.getAbsolutePath();
        if (!path.toLowerCase().matches(".*\\.(s|asm|txt)$")) {
             fileToSave = new File(path + ".s"); // Append .s if no valid extension
        }

        // Confirm overwrite if file exists
        if (fileToSave.exists()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "The file '" + fileToSave.getName() + "' already exists.\nDo you want to overwrite it?",
                "Confirm Overwrite",
                JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return false; // User chose not to overwrite
            }
        }

        // Perform the actual save
        if (performSave(fileToSave)) {
            currentFile = fileToSave; // Update current file reference
            markChangesSaved(); // Update title only after successful save
            return true;
        } else {
            return false; // Save failed
        }
    }

    private boolean performSave(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            textArea.write(writer); // Efficiently writes text area content to file
            originalContent = textArea.getText(); // Update original content marker
             System.out.println("Saved file: " + file.getAbsolutePath());
            // No need to call markChangesSaved here, it's called by saveFile/saveFileAs upon success
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + file.getName() + "\n" + e.getMessage(), "File Save Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Checks for unsaved changes and prompts the user to save if necessary. */
    private boolean promptToSave() {
        if (!hasUnsavedChanges) {
            return true; // No changes, safe to proceed
        }
        int choice = JOptionPane.showConfirmDialog(this,
            "Do you want to save the changes?",
            "Unsaved Changes",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            return saveFile(); // Proceed only if save is successful
        } else if (choice == JOptionPane.NO_OPTION) {
            return true; // Proceed without saving
        } else { // CANCEL_OPTION or closed dialog
            return false; // Cancel the original operation
        }
    }

    // --- State Management ---

    private void markChanged() {
        if (!hasUnsavedChanges) {
            hasUnsavedChanges = true;
            updateTitle(); // Add "*" to title
        }
    }

    public void markChangesSaved() {
        if (hasUnsavedChanges) {
            hasUnsavedChanges = false;
            updateTitle(); // Remove "*" from title
        }
         // Also update title if filename changed (after Save As)
        updateTitle();
    }

    private void updateTitle() {
        String title = "LEGv8 Assembly Editor";
        if (currentFile != null) {
            title += " - " + currentFile.getName();
        } else {
            title += " - New File";
        }
        if (hasUnsavedChanges) {
            title += "*"; // Indicator for unsaved changes
        }
        setTitle(title);
    }

    // Getter for main app to potentially get code if needed elsewhere
    public String getSourceCode() {
        return textArea.getText();
    }
}