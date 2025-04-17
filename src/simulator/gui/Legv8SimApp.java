package simulator.gui;

import simulator.assembler.Assembler;
import simulator.core.*; // Core components
import simulator.exceptions.AssemblyException;
import simulator.instructions.Instruction;
import simulator.instructions.InstructionConfigLoader;
import simulator.instructions.InstructionFactory;
import simulator.storage.*; // Storage components

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.BitSet; // Cần nếu dùng AssemblyEditor cũ trả về BitSet

public class Legv8SimApp extends JFrame implements ActionListener {

    // --- GUI Components ---
    private JButton btnLoadFileToEditor; // Thay cho btnLoadFileToEditor
    private JButton btnNewOrEditAssembly;  // Thay cho btnNewOrEditAssembly
    private JButton btnLoadConfig;
    private JButton btnStartSimulation;
    private JButton btnExit;
    private JLabel lblStatus;
    private JFileChooser fileChooser;

    // --- Child Windows ---
    // Giữ AssemblyEditor nếu vẫn muốn dùng nó
    private AssemblyEditor assemblyEditor = null; // Luôn dùng editor này
    private SimulationView simulationView = null; // Giữ nguyên

    // --- Core Simulation Components ---
    private InstructionConfigLoader configLoader;
    private Assembler assembler;
    private List<Instruction> loadedInstructions = null; // Lưu trữ lệnh đã assemble
    private Legv8Simulator simulatorEngine = null; // Sẽ tạo khi cần

    // --- State ---
    private boolean configReady = false;
    private boolean assemblyReady = false;
    private File lastAssemblyFile = null;

    public Legv8SimApp() {
        setTitle("LEGv8 Simulator Main");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Xử lý exit riêng
        setSize(400, 300);
        setLocationRelativeTo(null); // Center screen
        setLayout(new BorderLayout(10, 10));

        // Initialize core non-GUI components
        configLoader = new InstructionConfigLoader();
        assembler = new Assembler(); // Use default base address
        fileChooser = new JFileChooser("."); // File chooser starting in current directory

        initComponents();
        layoutComponents();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });
        updateSimulationButtonState(); // Set initial button state
    }

    private void initComponents() {
        lblStatus = new JLabel("Status: Load Config and Assembly", SwingConstants.CENTER);
        lblStatus.setBorder(BorderFactory.createEtchedBorder());

        btnLoadFileToEditor = new JButton("Load Assembly File to Editor...");
        btnNewOrEditAssembly = new JButton("New / Edit Assembly...");
        btnStartSimulation = new JButton("Start Simulation");
        btnExit = new JButton("Exit Application");

        btnLoadConfig = new JButton("Load Instruction Config...");
        btnLoadFileToEditor = new JButton("Load Assembly File...");
        btnNewOrEditAssembly = new JButton("Edit Assembly..."); // Nút mới
        btnStartSimulation = new JButton("Start Simulation");
        btnExit = new JButton("Exit Application");

        btnLoadConfig.addActionListener(this);
        btnLoadFileToEditor.addActionListener(this);
        btnNewOrEditAssembly.addActionListener(this);
        btnStartSimulation.addActionListener(this);
        btnExit.addActionListener(this);

        // Tooltips
        btnLoadConfig.setToolTipText("Load the .csv file defining instruction formats and signals");
        btnLoadFileToEditor.setToolTipText("Open an assembly file (.s, .asm) in the editor");
        btnNewOrEditAssembly.setToolTipText("Open the assembly editor (for new code or current code)");

        // btnLoadConfig.setToolTipText("Load the .csv file defining instruction formats and signals");
        // btnNewOrEditAssembly.setToolTipText("Open the built-in assembly code editor");
        // btnLoadFileToEditor.setToolTipText("Assemble an assembly (.s, .asm) file directly");
        // btnStartSimulation.setToolTipText("Open the simulation window (requires Config and Assembly)");
    }

    private void layoutComponents() {
        // Title Panel (Optional)
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("LEGv8 Simulator", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(titlePanel, BorderLayout.NORTH);

        // Button Panel (GridLayout)
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 10, 10)); // 1 column, auto rows
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40)); // Padding
        buttonPanel.add(btnLoadConfig);
        buttonPanel.add(btnLoadFileToEditor);
        buttonPanel.add(btnNewOrEditAssembly); // Tạm ẩn Editor nếu chưa dùng
        buttonPanel.add(btnStartSimulation);
        buttonPanel.add(btnExit);
        add(buttonPanel, BorderLayout.CENTER);

        // Status Bar
        add(lblStatus, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == btnLoadConfig) {
            loadInstructionConfig();
        } else if (source == btnLoadFileToEditor) {
            loadAssemblyFromFile();
        } else if (source == btnNewOrEditAssembly) {
            openAssemblyEditor(null);
        } else if (source == btnStartSimulation) {
            startSimulation();
        } else if (source == btnExit) {
            handleExit();
        }
    }

    // --- Action Methods ---

    private void loadInstructionConfig() {
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Config Files (*.csv)", "csv");
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("Select Instruction Configuration File");

        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File configFile = fileChooser.getSelectedFile();
            // Cần lấy đường dẫn resource tương đối từ classpath
            // Giả sử người dùng chọn file trong thư mục project/resources/
            // Cách đơn giản nhất là yêu cầu file nằm trong classpath hoặc copy nó vào build
            String resourcePath = "/" + configFile.getName(); // Giả định nó nằm ở gốc resources -> gốc classpath
            System.out.println("Attempting to load config resource: " + resourcePath);

            configReady = false; // Reset flag
            if (configLoader.loadConfig(resourcePath)) {
                 try {
                    InstructionFactory.initialize(configLoader); // Quan trọng: Khởi tạo Factory
                    configReady = true;
                    lblStatus.setText("Status: Config loaded. Load Assembly.");
                    System.out.println("InstructionFactory initialized successfully.");
                 } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error initializing InstructionFactory:\n" + ex.getMessage(), "Factory Init Error", JOptionPane.ERROR_MESSAGE);
                     lblStatus.setText("Status: Config loaded, but Factory init failed.");
                 }
            } else {
                JOptionPane.showMessageDialog(this, "Failed to load Config from " + configFile.getName() + "\nCheck console for details.", "Config Error", JOptionPane.ERROR_MESSAGE);
                lblStatus.setText("Status: Error loading Instruction Config.");
            }
            updateSimulationButtonState();
        }
    }

    /** Mở FileChooser, đọc file và hiển thị nội dung lên AssemblyEditor. */
    public void loadAssemblyFileIntoEditor() {
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Assembly Files (*.s, *.asm)", "s", "asm");
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("Select Assembly File to Load into Editor");

        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // Đọc toàn bộ nội dung file thành một String
                String fileContent = Files.readString(Paths.get(selectedFile.getAbsolutePath()));
                openAssemblyEditor(fileContent); // Mở editor với nội dung file
                // Cập nhật trạng thái nếu cần (ví dụ: file đã được load vào editor)
                lblStatus.setText("Status: Loaded '" + selectedFile.getName() + "' into Editor. Assemble from Editor.");
                // Không tự động assemble ở đây nữa
                assemblyReady = false; // Cần assemble từ editor
                updateSimulationButtonState();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + selectedFile.getName() + "\n" + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Mở hoặc đưa AssemblyEditor lên trước. Có thể nhận nội dung ban đầu. */
    public void openAssemblyEditor(String initialContent) {
        if (assemblyEditor == null) {
            assemblyEditor = new AssemblyEditor(this); // Tạo mới nếu chưa có
        }
        if (initialContent != null) {
            assemblyEditor.setSourceCode(initialContent); // Đặt nội dung mới
            assemblyEditor.markChangesSaved(); // Coi như chưa sửa đổi sau khi load
        }
        assemblyEditor.setVisible(true); // Hiển thị hoặc đưa lên trước
        assemblyEditor.toFront();
    }


    private void loadAssemblyFromFile() {
        if (!configReady) { // Cần config trước khi assemble
             JOptionPane.showMessageDialog(this, "Please load the Instruction Configuration first.", "Config Required", JOptionPane.WARNING_MESSAGE);
             return;
        }

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Assembly Files (*.s, *.asm)", "s", "asm");
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("Select Assembly File");

        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastAssemblyFile = fileChooser.getSelectedFile();
            assembleFile(lastAssemblyFile);
        }
    }

     private void openAssemblyEditor() {
         // TODO: Tích hợp lại AssemblyEditor nếu cần
         JOptionPane.showMessageDialog(this, "Assembly Editor not yet fully integrated in this version.", "Info", JOptionPane.INFORMATION_MESSAGE);
         // if (assemblyEditor == null) { assemblyEditor = new AssemblyEditor(this); }
         // assemblyEditor.setVisible(true);
         // assemblyEditor.toFront();
     }

     // Hàm này được gọi từ loadAssemblyFromFile hoặc từ AssemblyEditor
     public void assembleCode(List<String> codeLines, String sourceName) {
        if (!configReady) {
            lblStatus.setText("Status: Error - Config not loaded.");
            // Hiển thị lỗi cho người dùng nếu cần
            JOptionPane.showMessageDialog(this, "Instruction Configuration not loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            return; // Should not happen if UI logic is correct
        }
        assemblyReady = false; // Reset flag
        loadedInstructions = null;
        updateSimulationButtonState();
        try {
            assembler.reset(); // Reset assembler state
            List<Instruction> newlyAssembled = assembler.assemble(codeLines); // Lưu vào biến tạm

            loadedInstructions = newlyAssembled; // Assemble
            assemblyReady = true;
            lblStatus.setText("Status: Assembly successful (" + sourceName + "). Ready to simulate.");
            System.out.println("Assembly successful from " + sourceName + ". " + loadedInstructions.size() + " instructions.");
            // Optional: Print assembled instructions to console for verification
            // InstructionMemory tempMem = new InstructionMemory();
            // tempMem.loadInstructions(loadedInstructions);
            // tempMem.displayMemorySummary();

            // *** THÊM LOGIC GỌI RELOAD VIEW ***
            // Nếu SimulationView đang mở, yêu cầu nó reload lệnh mới
            if (simulationView != null && simulationView.isVisible()) {
                System.out.println("Legv8SimApp: Notifying SimulationView to reload instructions...");
                simulationView.reloadInstructions(loadedInstructions); // Gọi hàm mới của view
            }
            // *** KẾT THÚC THÊM LOGIC ***

         } catch (AssemblyException e) {
             assemblyReady = false;
             loadedInstructions = null;
             // Hiển thị lỗi chi tiết
             JTextArea textArea = new JTextArea(e.getMessage()); // Exception message contains aggregated errors
             textArea.setEditable(false);
             textArea.setWrapStyleWord(true);
             textArea.setLineWrap(true);
             JScrollPane scrollPane = new JScrollPane(textArea);
             scrollPane.setPreferredSize(new Dimension(600, 200));
             JOptionPane.showMessageDialog(this, scrollPane, "Assembly Errors (" + sourceName + ")", JOptionPane.ERROR_MESSAGE);
             lblStatus.setText("Status: Assembly failed (" + sourceName + ").");
             System.err.println("Assembly failed.");
         } catch (Exception e) { // Catch unexpected factory/runtime errors during assembly
             assemblyReady = false;
             loadedInstructions = null;
             JOptionPane.showMessageDialog(this, "An unexpected error occurred during assembly:\n" + e.getMessage(), "Assembly Error", JOptionPane.ERROR_MESSAGE);
             lblStatus.setText("Status: Unexpected assembly error.");
             e.printStackTrace();
         }
         updateSimulationButtonState();
     }

    // Helper để đọc và assemble file
    private void assembleFile(File file) {
         if (file == null) return;
         List<String> lines = new ArrayList<>();
         try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
             String line;
             while ((line = reader.readLine()) != null) {
                 lines.add(line);
             }
             assembleCode(lines, file.getName()); // Gọi hàm assemble chung
         } catch (IOException e) {
             JOptionPane.showMessageDialog(this, "Error reading file: " + file.getName() + "\n" + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
             lblStatus.setText("Status: Error reading assembly file.");
             assemblyReady = false;
             loadedInstructions = null;
             updateSimulationButtonState();
         }
     }


    private void startSimulation() {
        if (!configReady || !assemblyReady || loadedInstructions == null || loadedInstructions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Load Config AND successfully Assemble Code first!", "Cannot Start Simulation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- Create or Reset Simulator Engine ---
        try {
            // Create new components for a fresh simulation state
            System.out.println("Legv8SimApp: Creating new simulator engine...");

            ProgramCounter pc = new ProgramCounter(); // Resets to BASE_ADDRESS
            InstructionMemory imem = new InstructionMemory();
            imem.loadInstructions(loadedInstructions); // Load the assembled code

            ControlUnit cu = new ControlUnit(configLoader); // Reusable

            RegisterStorage rstore = new RegisterStorage(); // New empty registers
            RegisterFileController rfc = new RegisterFileController(rstore);

            MemoryStorage dstore = new MemoryStorage(); // New empty data memory
            DataMemoryController dmc = new DataMemoryController(dstore);

            ArithmeticLogicUnit alu = new ArithmeticLogicUnit(); // Reusable

            // Create the engine with fresh state components
            simulatorEngine = new Legv8Simulator(pc, imem, cu, rfc, alu, dmc);

            // --- Open Simulation View ---
            if (simulationView == null) {
                System.out.println("Legv8SimApp: Creating new SimulationView...");

                simulationView = new SimulationView(this, simulatorEngine);
            } else {
                System.out.println("Legv8SimApp: Updating existing SimulationView with new engine...");

                // If view exists, update its engine reference and reset its state
                simulationView.setSimulatorEngine(simulatorEngine); // This now also calls setComponents and resetSimulationState
            }

            simulationView.setVisible(true);
            simulationView.toFront(); // Bring to front
            // this.setVisible(false); // Optionally hide the main window

            lblStatus.setText("Status: Simulation window opened.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to initialize simulator engine or view:\n" + e.getMessage(), "Simulation Init Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            lblStatus.setText("Status: Failed to start simulation.");
            simulatorEngine = null; // Ensure engine is null on error
        }
        updateSimulationButtonState(); // Update button state (Sim button might disable if view opened?)
    }

    private void updateSimulationButtonState() {
        btnStartSimulation.setEnabled(configReady && assemblyReady);
    }

    private void handleExit() {
        // Optional: Add check for unsaved changes in AssemblyEditor if integrated
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to exit the LEGv8 Simulator?",
            "Confirm Exit",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Dispose child windows if they exist
            if (assemblyEditor != null) assemblyEditor.dispose();
            if (simulationView != null) simulationView.dispose();
            // Exit application
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        // Set Look and Feel (Optional)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel.");
        }

        // Run the application
        SwingUtilities.invokeLater(() -> {
            Legv8SimApp app = new Legv8SimApp();
            app.setVisible(true);
        });
    }
}