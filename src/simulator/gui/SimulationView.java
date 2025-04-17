package simulator.gui;

import simulator.assembler.Assembler;
import simulator.core.*; // Import core simulator classes
import simulator.storage.*; // Import storage classes
import simulator.instructions.*; // Import instruction classes
import simulator.exceptions.*; // Import exception classes
import simulator.gui.datapath.MicroStepInfo;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.List;

public class SimulationView extends JFrame implements ActionListener, ChangeListener /*, ItemListener */ { // ItemListener nếu dùng CheckBoxMenuItem

    private Legv8SimApp mainApp; // Tham chiếu về App chính (nếu cần)
    private Legv8Simulator simulatorEngine; // Tham chiếu đến engine mô phỏng

    // --- GUI Components ---
    private JToolBar toolBar; // Thanh công cụ mới
    private JButton btnLoadAssemblyTB; // Nút Load Assembly trên Toolbar
    private JButton btnRunTB, btnPauseTB, btnStopTB, btnStepTB; // Nút điều khiển trên Toolbar
    private JButton btnRegTB, btnMemTB, btnInstrTB; // Nút mở view trên Toolbar
    // private JButton btnRun, btnPause, btnStop, btnStepInstruction;
    // private JButton btnShowRegisters, btnShowMemory, btnShowInstructions;
    // private JButton btnCloseView;
    // Thêm lại nút Fast/Instant nếu muốn
    // private JButton btnSuperSpeed, btnInstantSpeed;
    private JSlider speedSlider;
    private JLabel lblSpeed;
    // private JPanel controlPanel; // Panel chứa các nút điều khiển
    private JPanel datapathPanel; // Panel sẽ chứa DatapathCanvas sau này (hiện tại là placeholder)
    private JLabel lblStatus;
    private JFileChooser fileChooser; // File chooser cho load assembly


    // --- Child View References ---
    private RegisterView registerView;
    private MemoryView memoryView;
    private InstructionView instructionView;

    // --- Simulation State ---
    private boolean isPaused = true;
    private Timer simulationTimer;
    private int simulationDelayMs = 200; // Default delay

    // --- References to display components (set via setComponents) ---
    private InstructionMemory instructionMemoryRef;
    private RegisterFileController registerControllerRef;
    private DataMemoryController memoryControllerRef;
    private ProgramCounter pcRef;

    public SimulationView(Legv8SimApp app, Legv8Simulator engine) {
        this.mainApp = app; // Lưu tham chiếu App chính
        Objects.requireNonNull(engine, "Simulator engine cannot be null for SimulationView"); // Nên truyền engine vào constructor
        // this.simulatorEngine = engine; // Nhận engine ngay khi tạo

        setTitle("LEGv8 Simulation");
        setSize(1600, 900); // Kích thước ban đầu
        setLocationRelativeTo(app); // Hiển thị gần cửa sổ chính
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Xử lý đóng cửa sổ riêng

        fileChooser = new JFileChooser("."); // Khởi tạo file chooser cho view này
        // Đặt thư mục làm việc mặc định nếu cần, ví dụ:
        // fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        
        initComponents();
        layoutComponents();

        // Khởi tạo Timer nhưng chưa start
        simulationTimer = new Timer(simulationDelayMs, e -> runSingleStep());
        simulationTimer.setInitialDelay(simulationDelayMs); // Delay trước lần chạy đầu tiên

        // Listener để xử lý việc đóng cửa sổ
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleCloseView();
            }
        });

        setSimulatorEngine(engine); // Gọi setter để khởi tạo trạng thái và component refs
        // Cần gọi setSimulatorEngine và setComponents từ bên ngoài sau khi tạo view này

        // updateControlState(); // Cập nhật trạng thái ban đầu của các nút
    }

     /**
      * Sets the simulator engine instance for this view.
      * Nên gọi phương thức này ngay sau khi tạo SimulationView.
      */
     public void setSimulatorEngine(Legv8Simulator engine) {
        Objects.requireNonNull(engine, "Simulator engine cannot be null.");
        System.out.println("SimulationView: Setting new simulator engine...");

        this.simulatorEngine = engine;
        // Lấy tham chiếu đến các thành phần từ engine mới
        setComponents(
            engine.getInstructionMemory(),
            engine.getRegisterController(),
            engine.getMemoryController(),
            engine.getPc()
        );
        resetSimulationState(); // Reset trạng thái GUI và cập nhật hiển thị ban đầu
        System.out.println("SimulationView: Engine and components updated.");
    }


    // Lưu trữ tham chiếu đến các thành phần cần hiển thị
    private void setComponents(InstructionMemory iMem, RegisterFileController rfc,
                              DataMemoryController dmc, ProgramCounter pc) {
        this.instructionMemoryRef = iMem;
        this.registerControllerRef = rfc;
        this.memoryControllerRef = dmc;
        this.pcRef = pc;
    }


    private void initComponents() {
        // --- Toolbar Components ---
        toolBar = new JToolBar("Simulation Controls");
        toolBar.setFloatable(false); // Không cho kéo toolbar ra ngoài

        btnLoadAssemblyTB = createToolBarButton("Load Assembly...", "folder_open.png", "LOAD_ASM", "Load a new assembly file (.s, .asm)");
        btnRunTB = createToolBarButton("Run", "run.png", "RUN", "Run simulation continuously");
        btnPauseTB = createToolBarButton("Pause", "pause.png", "PAUSE", "Pause/Resume simulation");
        btnStepTB = createToolBarButton("Step", "step.png", "STEP", "Execute one instruction cycle");
        btnStopTB = createToolBarButton("Reset", "reset.png", "RESET", "Stop and reset simulation state");

        btnRegTB = createToolBarButton("Registers", "regfile.png", "VIEW_REGS", "Show/Hide Register View");
        btnMemTB = createToolBarButton("Memory", "memory.png", "VIEW_MEM", "Show/Hide Data Memory View");
        btnInstrTB = createToolBarButton("Instructions", "imem.png", "VIEW_INSTR", "Show/Hide Instruction View");

        // Add components to toolbar
        toolBar.add(btnLoadAssemblyTB);
        toolBar.addSeparator();
        toolBar.add(btnRunTB);
        toolBar.add(btnPauseTB);
        toolBar.add(btnStepTB);
        toolBar.add(btnStopTB);
        toolBar.addSeparator();
        toolBar.add(btnRegTB);
        toolBar.add(btnMemTB);
        toolBar.add(btnInstrTB);
        toolBar.addSeparator();

        // Speed Control
        speedSlider = new JSlider(JSlider.HORIZONTAL, 0, 500, 500 - simulationDelayMs);
        speedSlider.setMaximumSize(new Dimension(150, speedSlider.getPreferredSize().height)); // Giới hạn chiều rộng
        speedSlider.setToolTipText("Simulation Speed (Slower <-> Faster)");
        speedSlider.addChangeListener(this);
        JLabel speedLabel = new JLabel(" Speed: ");
        toolBar.add(speedLabel);
        toolBar.add(speedSlider);

        
        // Status Label
        lblStatus = new JLabel("Status: Idle", JLabel.LEFT);
        lblStatus.setBorder(BorderFactory.createEtchedBorder());
        
        // Placeholder for Datapath Canvas
        datapathPanel = new JPanel(new BorderLayout());
        datapathPanel.setBackground(Color.DARK_GRAY);
        datapathPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        JLabel placeholder = new JLabel("Datapath Visualization Area", SwingConstants.CENTER);
        placeholder.setFont(new Font("SansSerif", Font.BOLD, 24));
        placeholder.setForeground(Color.LIGHT_GRAY);
        datapathPanel.add(placeholder);
        
        // // View Buttons
        // btnShowRegisters = new JButton("Registers");
        // btnShowMemory = new JButton("Data Memory");
        // btnShowInstructions = new JButton("Instructions");
        // btnCloseView = new JButton("Close Simulation");

        // // Add Action Listeners
        // btnRun.addActionListener(this);
        // btnPause.addActionListener(this);
        // btnStop.addActionListener(this);
        // btnStepInstruction.addActionListener(this);
        // speedSlider.addChangeListener(this);
        // btnShowRegisters.addActionListener(this);
        // btnShowMemory.addActionListener(this);
        // btnShowInstructions.addActionListener(this);
        // btnCloseView.addActionListener(this);

        // // Tooltips
        // btnRun.setToolTipText("Run the simulation continuously");
        // btnPause.setToolTipText("Pause or resume continuous execution");
        // btnStop.setToolTipText("Stop execution and reset the simulator state");
        // btnStepInstruction.setToolTipText("Execute one full instruction cycle");
        // btnShowRegisters.setToolTipText("Show/Hide the Register View window");
        // btnShowMemory.setToolTipText("Show/Hide the Data Memory View window");
        // btnShowInstructions.setToolTipText("Show/Hide the Instruction Memory View window");
        // btnCloseView.setToolTipText("Close this simulation window");
    }

    // Helper tạo nút cho toolbar (nên có icon)
    private JButton createToolBarButton(String text, String iconName, String actionCommand, String toolTip) {
        JButton button = new JButton(text);
        // Cố gắng load icon (giả sử icon trong /icons/ hoặc tương tự)
        try {
            //java.net.URL imgURL = getClass().getResource("/icons/" + iconName); // Đường dẫn tới icons
            //if (imgURL != null) {
            //    button.setIcon(new ImageIcon(imgURL));
            //    button.setText(""); // Ẩn text nếu có icon
            //}
        } catch (Exception ex) { System.err.println("Icon not found: " + iconName);}

        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTip);
        button.addActionListener(this);
        button.setFocusable(false); // Tránh toolbar giữ focus
        return button;
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(5, 5));
        add(toolBar, BorderLayout.NORTH); // Đặt toolbar ở trên cùng
        add(datapathPanel, BorderLayout.CENTER);
        add(lblStatus, BorderLayout.SOUTH);
        // controlPanel cũ không cần nữa nếu các nút đã lên toolbar
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command == null) return;

        switch (command) {
            case "LOAD_ASM": openEditorToLoadAssembly(); break;
            case "RUN": startSimulation(); break;
            case "PAUSE": pauseSimulation(); break;
            case "STEP": executeSingleInstruction(); break;
            case "RESET": stopAndResetSimulation(); break;
            case "VIEW_REGS": toggleRegisterView(); break;
            case "VIEW_MEM": toggleMemoryView(); break;
            case "VIEW_INSTR": toggleInstructionView(); break;
            // Thêm case cho các nút khác nếu có
            default:
                System.err.println("Unhandled action command: " + command);
        }
    }

        // --- Load Assembly Logic (Mới - Mở Editor) ---
    /** Mở Assembly Editor để người dùng có thể load file hoặc nhập code từ đó. */
    private void openEditorToLoadAssembly() {
        // 1. Dừng simulation nếu đang chạy và xác nhận (tùy chọn, có thể bỏ qua nếu chỉ mở editor)
        // boolean wasRunning = simulationTimer.isRunning();
        // if (wasRunning) { pauseSimulation(); }
        // int confirm = JOptionPane.showConfirmDialog(this, /* ... */ );
        // if (confirm != JOptionPane.YES_OPTION) { return; }

        // 2. *** Gọi App chính để mở Editor ***
        if (mainApp != null) {
            // Mở editor, không truyền nội dung ban đầu (để editor tự quản lý nội dung hiện tại của nó)
            mainApp.openAssemblyEditor(null);
            // Thông báo cho người dùng biết phải làm gì tiếp theo
            lblStatus.setText("Status: Assembly Editor opened. Use File menu in Editor to load or Assemble.");
        } else {
            showError("Cannot access main application to open editor.");
        }
        // Việc load file, assemble, và cập nhật simulator sẽ do người dùng thao tác
        // trong AssemblyEditor và sau đó là trong Legv8SimApp khi nhấn Start Simulation.
    }


    // --- Load Assembly Logic (Mới) ---
    /** Yêu cầu App chính mở FileChooser và load file vào Editor. */
    @Deprecated
    private void loadAssemblyIntoEditorViaMainApp() {
        // 1. Dừng simulation nếu đang chạy (như cũ)
        boolean wasRunning = simulationTimer.isRunning();
        if (wasRunning) { pauseSimulation(); }

        // 2. Hiển thị cảnh báo (như cũ)
        int confirm = JOptionPane.showConfirmDialog(this,
            "Loading a new assembly file will reset the current simulation state.\nDo you want to continue?",
            "Confirm Load New File",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            if (wasRunning) {
                    // Nếu trước đó đang chạy, có thể resume lại? Hoặc cứ để paused.
                    // pauseSimulation(); // Gọi lại để toggle về trạng thái chạy? Tùy ý.
                    System.out.println("Load new assembly cancelled.");
            }
            return; // Hủy load
        }
        // 3. *** Gọi lại App chính để xử lý việc chọn file và mở editor ***
        if (mainApp != null) {
            mainApp.loadAssemblyFileIntoEditor(); // Gọi hàm tương ứng của Legv8SimApp
            // Việc assemble và cập nhật simulator sẽ diễn ra sau khi người dùng nhấn Assemble trong Editor
            // và gọi lại mainApp.assembleCode(...) -> mainApp.startSimulation(...)
            // Hiện tại SimulationView không cần làm gì thêm ở đây.
            // Có thể cập nhật status?
            lblStatus.setText("Status: Opening Editor to load new Assembly...");
        } else {
            showError("Cannot access main application to load file.");
        }
    }

    // --- Load Assembly Logic ---
    @Deprecated
    private void loadAssemblyFile() {
        // 1. Dừng simulation hiện tại (nếu đang chạy) và xác nhận
        boolean wasRunning = simulationTimer.isRunning();
        if (wasRunning) {
            pauseSimulation(); // Tạm dừng trước
        }

        // 2. Hiển thị cảnh báo VÀ xác nhận
        int confirm = JOptionPane.showConfirmDialog(this,
            "Loading a new assembly file will reset the current simulation state.\nDo you want to continue?",
            "Confirm Load New File",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        // 3. Nếu người dùng KHÔNG chọn "Yes", hủy bỏ
        if (confirm != JOptionPane.YES_OPTION) {
            if (wasRunning) {
                 // Nếu trước đó đang chạy, có thể resume lại? Hoặc cứ để paused.
                 // pauseSimulation(); // Gọi lại để toggle về trạng thái chạy? Tùy ý.
                 System.out.println("Load new assembly cancelled.");
            }
            return; // Hủy load
        }

        // 4. *** CHỈ KHI người dùng chọn "Yes", mới hiển thị hộp thoại chọn file ***
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Assembly Files (*.s, *.asm)", "s", "asm");
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("Select Assembly File to Load");
        int returnVal = fileChooser.showOpenDialog(this);

        // 5. Nếu người dùng chọn file (không nhấn Cancel trong FileChooser)
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // 6. Đọc và Assemble (logic này giữ nguyên)
            List<String> codeLines;
            List<Instruction> newInstructions;
            try {
                codeLines = Files.readAllLines(Paths.get(selectedFile.getAbsolutePath())); // Cách đọc file mới
                Assembler localAssembler = new Assembler(); // Tạo assembler mới cho mỗi lần load
                // Cần InstructionFactory đã được khởi tạo (giả sử mainApp đã làm)
                if (!InstructionFactory.isInitialized()) { // Cần thêm hàm isInitialized() vào Factory
                    showError("InstructionFactory not initialized. Cannot assemble.");
                    return;
                }
                newInstructions = localAssembler.assemble(codeLines); // Assemble
            } catch (IOException ioEx) {
                showError("Error reading file '" + selectedFile.getName() + "':\n" + ioEx.getMessage());
                return;
            } catch (AssemblyException asmEx) {
                // Hiển thị lỗi chi tiết
                JTextArea textArea = new JTextArea(asmEx.getMessage()); textArea.setEditable(false); /*...*/
                JScrollPane scrollPane = new JScrollPane(textArea); scrollPane.setPreferredSize(new Dimension(600, 200));
                JOptionPane.showMessageDialog(this, scrollPane, "Assembly Errors (" + selectedFile.getName() + ")", JOptionPane.ERROR_MESSAGE);
                return;
            } catch (Exception ex) { // Lỗi khác
                showError("An unexpected error occurred during assembly:\n" + ex.getMessage());
                ex.printStackTrace();
                return;
            }
            
            // 7. Reset Simulator và Load lệnh mới
            if (simulatorEngine != null && newInstructions != null) {
                stopAndResetSimulation(); // Dừng và reset engine hiện tại
                
                // Load lệnh mới vào Instruction Memory của engine
                try {
                    simulatorEngine.getInstructionMemory().loadInstructions(newInstructions);
                    lblStatus.setText("Status: New assembly loaded & reset. Ready.");
                    System.out.println("Loaded " + newInstructions.size() + " instructions from " + selectedFile.getName());
                    updateAllStateViews(); // Cập nhật các view với trạng thái reset và lệnh mới
                } catch (Exception loadEx) {
                    showError("Error loading new instructions into memory:\n" + loadEx.getMessage());
                    lblStatus.setText("Status: Error loading new instructions.");
                }
            } else if (newInstructions != null) {
                // Trường hợp chưa có engine, chỉ cần báo thành công, engine sẽ được tạo khi nhấn Start
                lblStatus.setText("Status: Assembly loaded ("+selectedFile.getName()+"). Ready to start simulation.");
                // Có thể lưu newInstructions vào một biến tạm để startSimulation dùng
            }
            updateControlState();
        } else {
            System.out.println("File selection cancelled.");
            // Nếu trước đó đang chạy, có thể resume lại ở đây nếu muốn
            // if (wasRunning) { pauseSimulation(); }
        }
    }


    @Override
    public void stateChanged(ChangeEvent e) { 
        if (e.getSource() == speedSlider && !speedSlider.getValueIsAdjusting()) {
            int sliderValue = speedSlider.getValue();
            int maxDelay = 501; int minDelay = 1;
            simulationDelayMs = maxDelay - (sliderValue * (maxDelay - minDelay) / 500);
            simulationDelayMs = Math.max(minDelay, simulationDelayMs);
            simulationTimer.setDelay(simulationDelayMs);
            simulationTimer.setInitialDelay(simulationDelayMs);
        }
    }

        // --- State Updates (updateAllStateViews, updateControlState, resetSimulationState) ---
    // Cần cập nhật updateControlState để enable/disable các nút toolbar mới (btnRunTB,...)
    private void updateControlState() {
        boolean canRun = false; boolean canPause = false; boolean canResume = false;
        boolean canStep = false; boolean canStopReset = false;
        boolean engineExists = (simulatorEngine != null);
        boolean isRunning = engineExists && !isPaused && simulationTimer.isRunning();
        boolean isHalted = !engineExists || simulatorEngine.isHalted();
        boolean isEffectivelyPaused = isPaused; // Dừng do pause hoặc chưa chạy

        if (engineExists) {
            canRun = !isRunning && !isHalted;
            canPause = isRunning;
            canResume = isEffectivelyPaused && !isHalted;
            canStep = isEffectivelyPaused && !isHalted;
            canStopReset = true; // Luôn có thể reset nếu có engine
        } else {
            canStopReset = false; // Không reset nếu không có engine
        }

        btnRunTB.setEnabled(canRun);
        btnPauseTB.setEnabled(canPause || canResume);
        btnPauseTB.setText(isPaused ? "Resume" : "Pause"); // Đặt lại Text nếu cần, hoặc chỉ dùng icon
        btnPauseTB.setToolTipText(isPaused ? (canResume ? "Resume execution" : "Cannot resume") : (canPause ? "Pause execution" : ""));
        btnStepTB.setEnabled(canStep);
        btnStopTB.setEnabled(canStopReset);

        // Các nút mở view luôn bật
        btnRegTB.setEnabled(true);
        btnMemTB.setEnabled(true);
        btnInstrTB.setEnabled(true);
        btnLoadAssemblyTB.setEnabled(true); // Luôn cho phép load file mới
    }

        /**
     * Reloads the instruction memory of the current simulator engine with new instructions.
     * This method automatically stops and resets the simulation state before loading.
     * Called by Legv8SimApp after successful assembly from the editor.
     *
     * @param newInstructions The list of newly assembled Instruction objects.
     */
    public void reloadInstructions(List<Instruction> newInstructions) {
        System.out.println("SimulationView: Received request to reload instructions...");
        if (simulatorEngine == null) {
            showError("Cannot reload instructions: Simulator engine is not active.");
            return;
        }
        if (newInstructions == null) {
             showError("Cannot reload instructions: Instruction list is null.");
             return;
        }

        // 1. Dừng và Reset trạng thái engine và GUI
        stopAndResetSimulation(); // Hàm này đã bao gồm dừng timer, reset engine, cập nhật GUI

        // 2. Load lệnh mới vào bộ nhớ của engine ĐÃ ĐƯỢC RESET
        try {
             simulatorEngine.getInstructionMemory().loadInstructions(newInstructions);
             lblStatus.setText("Status: Assembly reloaded & reset. Ready.");
             System.out.println("Reloaded " + newInstructions.size() + " instructions.");

             // 3. Cập nhật lại các view (đặc biệt là InstructionView) để hiển thị lệnh mới
             updateAllStateViews(); // Gọi lại để đảm bảo view lệnh được cập nhật

        } catch (Exception loadEx) {
             showError("Error loading re-assembled instructions into memory:\n" + loadEx.getMessage());
             lblStatus.setText("Status: Error reloading instructions.");
        }

        // 4. Cập nhật trạng thái nút (vẫn sẽ là trạng thái idle/reset)
        updateControlState();
    }
    
    // private void layoutComponents() {
    //     // Control Panel Layout (Using GridBagLayout for flexibility)
    //     controlPanel = new JPanel(new GridBagLayout());
    //     controlPanel.setBorder(BorderFactory.createCompoundBorder(
    //         BorderFactory.createTitledBorder("Controls"),
    //         BorderFactory.createEmptyBorder(5, 5, 5, 5)
    //     ));
    //     GridBagConstraints gbc = new GridBagConstraints();
    //     gbc.gridx = 0;
    //     gbc.gridy = GridBagConstraints.RELATIVE; // Place components vertically
    //     gbc.gridwidth = 1;
    //     gbc.fill = GridBagConstraints.HORIZONTAL;
    //     gbc.insets = new Insets(2, 5, 2, 5); // Padding

    //     // Execution Buttons
    //     JPanel execPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
    //     execPanel.add(btnRun);
    //     execPanel.add(btnPause);
    //     execPanel.add(btnStop);
    //     controlPanel.add(execPanel, gbc);

    //     // Step Button
    //     controlPanel.add(btnStepInstruction, gbc);

    //     // Separator
    //     gbc.insets = new Insets(10, 0, 10, 0); // More vertical padding for separator
    //     controlPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
    //     gbc.insets = new Insets(2, 5, 2, 5); // Reset insets

    //     // View Buttons
    //     controlPanel.add(btnShowRegisters, gbc);
    //     controlPanel.add(btnShowMemory, gbc);
    //     controlPanel.add(btnShowInstructions, gbc);

    //     // Separator
    //     gbc.insets = new Insets(10, 0, 10, 0);
    //     controlPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
    //     gbc.insets = new Insets(2, 5, 2, 5);

    //     // Speed Control
        // JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        // // speedPanel.add(lblSpeed); // Label có thể không cần thiết
        // controlPanel.add(speedSlider, gbc);

    //     // Separator
    //     gbc.insets = new Insets(10, 0, 10, 0);
    //     controlPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
    //     gbc.insets = new Insets(2, 5, 2, 5);

    //     // Close Button (Pushes other components up)
    //     gbc.weighty = 1.0; // Make this component take extra vertical space
    //     gbc.anchor = GridBagConstraints.SOUTH; // Anchor it to the bottom
    //     controlPanel.add(btnCloseView, gbc);


    //     // Main Layout
    //     setLayout(new BorderLayout(5, 5));
    //     add(datapathPanel, BorderLayout.CENTER);
    //     add(controlPanel, BorderLayout.EAST);
    //     add(lblStatus, BorderLayout.SOUTH);

    //     // TODO: Add Menu Bar if needed
    //     // setJMenuBar(createMenuBar());
    // }


    // @Override
    // public void actionPerformed(ActionEvent e) {
    //     Object src = e.getSource();
    //     if (src == btnRun) {
    //         startSimulation();
    //     } else if (src == btnPause) {
    //         pauseSimulation();
    //     } else if (src == btnStop) {
    //         stopAndResetSimulation();
    //     } else if (src == btnStepInstruction) {
    //         executeSingleInstruction();
    //     } else if (src == btnShowRegisters) {
    //         toggleRegisterView();
    //     } else if (src == btnShowMemory) {
    //         toggleMemoryView();
    //     } else if (src == btnShowInstructions) {
    //         toggleInstructionView();
    //     } else if (src == btnCloseView) {
    //         handleCloseView();
    //     }
    // }

    // @Override
    // public void stateChanged(ChangeEvent e) {
    //     if (e.getSource() == speedSlider && !speedSlider.getValueIsAdjusting()) {
    //         // Value 0 = max delay, Value 500 = min delay (close to 0)
    //         int sliderValue = speedSlider.getValue();
    //         // Map slider value (0-500) to delay (e.g., 500ms - 1ms)
    //         // Simple linear mapping: delay = MaxDelay - (sliderValue * (MaxDelay - MinDelay) / MaxSliderValue)
    //         int maxDelay = 501; // ms
    //         int minDelay = 1;   // ms
    //         simulationDelayMs = maxDelay - (sliderValue * (maxDelay - minDelay) / 500);
    //         simulationDelayMs = Math.max(minDelay, simulationDelayMs); // Ensure minimum delay

    //         simulationTimer.setDelay(simulationDelayMs);
    //         simulationTimer.setInitialDelay(simulationDelayMs); // Update initial delay too
    //         // System.out.println("Simulation Delay set to: " + simulationDelayMs + " ms (Slider: " + sliderValue + ")");
    //     }
    // }

    // --- Simulation Control Methods ---

    private void startSimulation() {
        if (simulatorEngine == null) {
            showError("Simulator engine not initialized.");
            return;
        }
        if (simulatorEngine.isHalted()) {
            lblStatus.setText("Status: Halted. Reset simulator to run again.");
            return;
        }
        if (!isPaused && simulationTimer.isRunning()) {
            return; // Already running
        }
        isPaused = false;
        lblStatus.setText("Status: Running...");
        updateControlState();
        simulationTimer.start();
    }

    private void pauseSimulation() {
        if (simulatorEngine == null) return;

        if (simulationTimer.isRunning()) {
            simulationTimer.stop();
            isPaused = true;
            lblStatus.setText("Status: Paused.");
        } else if (isPaused && !simulatorEngine.isHalted()) {
             // If paused but not running (e.g., after stepping), resume
            isPaused = false;
            lblStatus.setText("Status: Resuming...");
            simulationTimer.start(); // Start timer to continue
        }
        updateControlState();
    }

    private void stopAndResetSimulation() {
        simulationTimer.stop(); // Stop timer first
        isPaused = true;
        if (simulatorEngine != null) {
            simulatorEngine.reset(); // Reset the engine's state
            updateAllStateViews();   // Update views to show reset state
            lblStatus.setText("Status: Stopped / Reset.");
        } else {
            lblStatus.setText("Status: Idle (No engine).");
        }
        updateControlState();
    }

    // Called by Step button or Timer
    private void runSingleStep() {
        if (simulatorEngine == null || simulatorEngine.isHalted()) {
            simulationTimer.stop();
            isPaused = true;
            updateControlState();
            lblStatus.setText(simulatorEngine == null ? "Status: Error (No Engine)" : "Status: Halted.");
            return;
        }

         try {
            List<MicroStepInfo> microSteps = simulatorEngine.stepAndGetMicroSteps(); // Gọi hàm mới
            boolean canContinue = !simulatorEngine.isHalted(); // Kiểm tra trạng thái halt sau khi step

            updateAllStateViews(); // Cập nhật các view trạng thái (Registers, Memory, etc.)

            // TODO: Xử lý danh sách microSteps để cập nhật DatapathCanvas sau này
            // Ví dụ:
            // if (datapathCanvas != null && !microSteps.isEmpty()) {
            //    // Hiển thị micro-step cuối cùng hoặc xử lý tuần tự
            //    datapathCanvas.updateState(microSteps.get(microSteps.size() - 1));
            // }

            if (!canContinue) { // Simulator halted itself during the step
                simulationTimer.stop();
                isPaused = true;
                lblStatus.setText("Status: Halted.");
            } else if (isPaused) { // If we were manually stepping
                lblStatus.setText("Status: Paused after step.");
            } else {
                // Still running continuously, status updated by startSimulation/pauseSimulation
            }

        } catch (SimulationException simEx) {
            simulationTimer.stop();
            isPaused = true;
            showError("Simulation Error: " + simEx.getMessage());
            lblStatus.setText("Status: Halted (Error).");
            updateAllStateViews(); // Cập nhật view lần cuối khi lỗi
        } catch (Exception ex) { // Catch unexpected errors
            simulationTimer.stop();
            isPaused = true;
            showError("Unexpected Runtime Error: " + ex.getMessage());
            ex.printStackTrace();
            lblStatus.setText("Status: Halted (Runtime Error).");
            updateAllStateViews();
        }
        updateControlState(); // Cập nhật trạng thái nút
    }

    // Action for the "Step Instruction" button
    private void executeSingleInstruction() {
        if (simulatorEngine == null) {
            showError("Simulator engine not initialized.");
            return;
        }
         if (!isPaused) {
             // If running, pause first before stepping
             pauseSimulation();
             // Optional: Add a small delay here if needed, but usually pausing is enough
         }
         if (simulatorEngine.isHalted()) {
             lblStatus.setText("Status: Halted. Reset to step.");
             return;
         }
        lblStatus.setText("Status: Stepping...");
        // Run the step logic directly
        runSingleStep();
        // Ensure state is Paused after stepping manually
        if (!simulatorEngine.isHalted()) {
            isPaused = true;
            lblStatus.setText("Status: Paused after step.");
            updateControlState();
        }
    }

    // --- View Management ---

    private void toggleRegisterView() {
         if (registerControllerRef == null) { showError("Register data not available."); return; }
         if (registerView == null) {
             registerView = new RegisterView(this, registerControllerRef.getStorage());
             registerView.updateData(registerControllerRef.getStorage(), -1); // Initial data
         }
         registerView.setVisible(!registerView.isVisible());
         if (registerView.isVisible()) registerView.toFront();
     }

     private void toggleMemoryView() {
        if (memoryControllerRef == null) { showError("Memory data not available."); return; }
         if (memoryView == null) {
             memoryView = new MemoryView(this, memoryControllerRef.getStorage());
             memoryView.updateData(memoryControllerRef.getStorage(), -1L); // Initial data
         }
         memoryView.setVisible(!memoryView.isVisible());
         if (memoryView.isVisible()) memoryView.toFront();
     }

     private void toggleInstructionView() {
        if (instructionMemoryRef == null || pcRef == null) { showError("Instruction/PC data not available."); return; }
        if (instructionView == null) {
            System.out.println("Creating new InstructionView..."); // Log
            instructionView = new InstructionView(this, instructionMemoryRef); // Tạo view, constructor sẽ set column names
            //  instructionView.updateData(instructionMemoryRef, pcRef.getCurrentAddress()); // Initial data & highlight
        }
        //  instructionView.setVisible(!instructionView.isVisible());
        //   if (instructionView.isVisible()) {
        //       instructionView.toFront();
        //       instructionView.highlightPCRow(pcRef.getCurrentAddress()); // Re-highlight on show
        //   }
        if (!instructionView.isVisible()) {
            System.out.println("Setting InstructionView visible and updating data..."); // Log
            // Cập nhật dữ liệu NGAY TRƯỚC KHI hiển thị hoặc ngay sau đó
            instructionView.updateData(instructionMemoryRef, pcRef.getCurrentAddress());
            instructionView.setVisible(true); // Hiển thị
        } else {
             System.out.println("Hiding InstructionView..."); // Log
            instructionView.setVisible(false); // Chỉ ẩn đi
        }
        if (instructionView.isVisible()) instructionView.toFront();
    }

    // --- State Updates ---

    /** Updates all associated state views (Registers, Memory, Instructions). */
    private void updateAllStateViews() {
        if (simulatorEngine == null) return; // Cannot update if no engine

        // Update Registers if view exists and is visible
        if (registerView != null && registerView.isVisible() && registerControllerRef != null) {
            // Need last changed register index from the controller/simulator if available
            // For now, pass -1 if we don't track it precisely in the controller yet
            registerView.updateData(registerControllerRef.getStorage(), -1 /* TODO: Get last written index */);
        }
        // Update Memory if view exists and is visible
        if (memoryView != null && memoryView.isVisible() && memoryControllerRef != null) {
             // Need last accessed memory address from controller/simulator if available
             memoryView.updateData(memoryControllerRef.getStorage(), -1L /* TODO: Get last accessed address */);
        }
        // Update Instructions if view exists and is visible
        if (instructionView != null && instructionView.isVisible() && instructionMemoryRef != null && pcRef != null) {
            instructionView.updateData(instructionMemoryRef, pcRef.getCurrentAddress());
        }

         // TODO: Update DatapathCanvas based on MicroStepInfo if implementing detailed view
         if (datapathPanel != null) {
            // datapathPanel.updateState(...); // Requires MicroStep logic
            datapathPanel.repaint(); // Basic repaint for now
         }
    }

    // /** Updates the enabled/disabled state of control buttons. */
    // private void updateControlState() {
    //     boolean canRun = false; // Giả sử không thể chạy ban đầu
    //     boolean canPause = false;
    //     boolean canResume = false;
    //     boolean canStep = false;
    //     boolean canStopReset = false;
    
    //     if (simulatorEngine != null) { // Chỉ cập nhật nếu có engine
    //         boolean isRunning = !isPaused && simulationTimer.isRunning();
    //         boolean isEffectivelyPaused = isPaused; // Đang ở trạng thái dừng (do pause hoặc chưa start)
    //         boolean isHalted = simulatorEngine.isHalted();
    
    //         canRun = !isRunning && !isHalted; // Có thể Run nếu không chạy và không halt
    //         canPause = isRunning;             // Có thể Pause nếu đang chạy
    //         canResume = isEffectivelyPaused && !isHalted; // Có thể Resume nếu đang dừng và không halt
    //         canStep = isEffectivelyPaused && !isHalted;  // Có thể Step nếu đang dừng và không halt
    //         canStopReset = true; // Luôn có thể thử Stop/Reset
    //     } else {
    //         // Nếu không có engine, vô hiệu hóa hết trừ Stop/Reset (có thể dùng để đóng?)
    //         canStopReset = true;
    //     }
    
    
    //     btnRun.setEnabled(canRun);
    //     btnPause.setEnabled(canPause || canResume); // Bật nếu có thể Pause hoặc Resume
    //     btnPause.setText(isPaused ? "Resume" : "Pause");
    //     btnStop.setEnabled(canStopReset);
    //     btnStepInstruction.setEnabled(canStep);
    
    //     // Cập nhật lại tooltip cho nút Pause/Resume để rõ ràng hơn
    //     if (isPaused) {
    //         btnPause.setToolTipText(canResume ? "Resume continuous execution" : "Cannot resume (Halted or no engine)");
    //     } else {
    //         btnPause.setToolTipText(canPause ? "Pause continuous execution" : ""); // Không cần tooltip khi đang chạy?
    //     }
    // }

     /** Resets the GUI state, typically after engine reset or when setting a new engine. */
    public void resetSimulationState() {
        simulationTimer.stop();
        isPaused = true;
        lblStatus.setText(simulatorEngine != null ? "Status: Reset / Idle" : "Status: No Engine");
        updateControlState();
        updateAllStateViews(); // Show the reset state in views
    }

    // --- Utility ---

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Simulation Error", JOptionPane.ERROR_MESSAGE);
        lblStatus.setText("Status: Error");
    }

    private void handleCloseView() {
        simulationTimer.stop(); // Stop simulation timer
        // Dispose child windows first
        if (registerView != null) registerView.dispose();
        if (memoryView != null) memoryView.dispose();
        if (instructionView != null) instructionView.dispose();
        // Hide this window
        setVisible(false);
        // Optionally notify the main application
        if (mainApp != null) {
            // mainApp.simulationViewClosed(); // Example callback method
            mainApp.setVisible(true); // Show main app again
        }
    }
}