package legv8.gui;

import legv8.assembler.Assembler; 
import legv8.core.InstructionMemory; 
import legv8.instructions.InstructionConfigLoader;
import legv8.util.ColoredLog;
import legv8.instructions.Instruction; 

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.BitSet; 

public class AssemblyEditor extends JFrame implements ActionListener {

    private JTextArea textArea;
    private JScrollPane scrollPane;
    private JButton btnAssemble;    
    private JButton btnClose;   
    private JFileChooser fileChooser;
    private File currentFile;
    private JMenuItem miNew, miOpen, miSave, miSaveAs, miExitEditor;

    private String content = "";
    private boolean hasUnsavedChanges = false;
    
    private List<Instruction> instructions = null; 
    private Assembler assembler = new Assembler(); 
    private InstructionConfigLoader configLoader = new InstructionConfigLoader(); 

    private Font textFont = new Font("Monospaced", Font.PLAIN, 14);

    public AssemblyEditor() {
        setTitle("LEGv8 Assembly Editor");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
        updateTitle();

        
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
           @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { markChanged(); }
           @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { markChanged(); }
           @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { /* Style change, Implementing later */ }
        });

        
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { handleClose(); }
        });
    }

    public AssemblyEditor(InstructionConfigLoader configLoader) {
        this();
        this.configLoader = configLoader; 
    }

    private void initComponents() {
        textArea = new JTextArea();
        textArea.setFont(textFont);
        textArea.setTabSize(4);
        textArea.setMargin(new Insets(5, 5, 5, 5));
        scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        btnAssemble = new JButton("Assemble"); 
        btnClose = new JButton("Close");

        btnAssemble.setToolTipText("Assemble the current code");
        btnClose.setToolTipText("Hide this editor window");

        btnAssemble.addActionListener(this);
        btnClose.addActionListener(this);

        fileChooser = new JFileChooser("."); 
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Assembly Files (*.s, *.asm, *.txt)", "s", "asm", "txt");
        fileChooser.setFileFilter(filter);

        currentFile = null;
        content = "";
        hasUnsavedChanges = false;
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu fm = new JMenu("File");

        miNew = new JMenuItem("New");
        miOpen = new JMenuItem("Open...");
        miSave = new JMenuItem("Save");
        miSaveAs = new JMenuItem("Save As...");
        miExitEditor = new JMenuItem("Hide Window"); 

        miNew.addActionListener(this);
        miOpen.addActionListener(this);
        miSave.addActionListener(this);
        miSaveAs.addActionListener(this);
        miExitEditor.addActionListener(this);

        
        miNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        miOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        miSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        miExitEditor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx())); 

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
            assembleCurrentCode(); 
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

    private void assembleCurrentCode() {
        System.out.println(ColoredLog.PENDING + "Assembly Editor: Assemble requested.");
        String code = textArea.getText();
        List<String> codeLines = Arrays.asList(code.split("\\r?\\n")); 

        
        boolean proceed = true;
        if (hasUnsavedChanges) {
            int choice = JOptionPane.showConfirmDialog(this,
                "There are unsaved changes. Save before assembling?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                proceed = saveFile(); 
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                proceed = false; 
            }
            
        }

        if (proceed) {
            instructions = assembler.assemble(codeLines);
            if (instructions != null) {
                
                JOptionPane.showMessageDialog(this, "Assembly successful! " + instructions.size() + " instructions generated.", "Assembly Success", JOptionPane.INFORMATION_MESSAGE);
                for (Instruction instruction : instructions) {
                    System.out.println(instruction); 
                }
            } else {
                
                JOptionPane.showMessageDialog(this, "Assembly failed. Check the code for errors.", "Assembly Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleClose() {
        if (promptToSave()) {
            setVisible(false); 
        }
    }

    

    private void newFile() {
        if (promptToSave()) {
            textArea.setText("");
            currentFile = null;
            content = "";
            markChangesSaved(); 
            textArea.setCaretPosition(0); 
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
            textArea.read(reader, null); 
            currentFile = file;
            content = textArea.getText();
            markChangesSaved();
            textArea.setCaretPosition(0); 
            System.out.println(ColoredLog.SUCCESS + "Opened file: " + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + file.getName() + "\n" + e.getMessage(), "File Read Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean saveFile() {
        if (currentFile == null) {
            return saveFileAs(); 
        } else {
            return performSave(currentFile);
        }
    }

    private boolean saveFileAs() {
        if (currentFile != null) {
            fileChooser.setSelectedFile(currentFile); 
        } else {
            fileChooser.setSelectedFile(new File("untitled.s")); 
        }

        int returnVal = fileChooser.showSaveDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return false; 
        }

        File fileToSave = fileChooser.getSelectedFile();
        
        String path = fileToSave.getAbsolutePath();
        if (!path.toLowerCase().matches(".*\\.(s|asm|txt)$")) {
             fileToSave = new File(path + ".s"); 
        }

        
        if (fileToSave.exists()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "The file '" + fileToSave.getName() + "' already exists.\nDo you want to overwrite it?",
                "Confirm Overwrite",
                JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return false; 
            }
        }

        
        if (performSave(fileToSave)) {
            currentFile = fileToSave; 
            markChangesSaved(); 
            return true;
        } else {
            return false; 
        }
    }

    private boolean performSave(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            textArea.write(writer); 
            content = textArea.getText(); 
            System.out.println(ColoredLog.SUCCESS + "Saved file: " + file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + file.getName() + "\n" + e.getMessage(), "File Save Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean promptToSave() {
        if (!hasUnsavedChanges) {
            return true; 
        }
        int choice = JOptionPane.showConfirmDialog(this,
            "Do you want to save the changes?",
            "Unsaved Changes",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            return saveFile(); 
        } else if (choice == JOptionPane.NO_OPTION) {
            return true; 
        } else { 
            return false; 
        }
    }

    

    private void markChanged() {
        if (!hasUnsavedChanges) {
            hasUnsavedChanges = true;
            updateTitle(); 
        }
    }

    private void markChangesSaved() {
        if (hasUnsavedChanges) {
            hasUnsavedChanges = false;
            updateTitle(); 
        }
         
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
            title += "*"; 
        }
        setTitle(title);
    }

    
    public String getSourceCode() {
        return textArea.getText();
    }

    public List<Instruction> getAssembledInstructions() {
        return instructions;
    }
}