/**
 * @author TrDoanh
 * @version 1.0 --- Maybe exist bugs :)
 */

import legv8.instructions.*;
import legv8.simulator.SimulatorEngine;
import legv8.util.ColoredLog;
import legv8.gui.*;
import legv8.core.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

/**
 * Main application window and entry point for the LEGv8 simulator.
 * Handles loading configuration, opening the assembly editor,
 * and launching the simulation view.
 */
public class Application extends JFrame implements ActionListener {
    // --- GUI Components ---
    private JButton btnEditAssembly;     
    private JButton btnLoadConfig;
    private JButton btnStartSimulation;
    private JButton btnExit;
    private JLabel lblStatus;
    private JFileChooser fileChooser;

    // --- Child Windows/Views ---
    private AssemblyEditor assemblyEditor;
    private SimulationView simulationView;

    // --- Core Logic & Data ---
    private InstructionConfigLoader configLoader;
    private List<Instruction> loadedInstructions = null; 
    private SimulatorEngine simulatorEngine = null; 

    // --- Fonts ---
    Font titleFont;
    Font buttonFont;

    //     setIconImage(new ImageIcon(getClass().getResource("/resources/icons/legv8_icon.png")));


    // --- Constructor ---

    /**
     * Constructs the main application frame, initializing components and controllers.
     */
    public Application() {
        setTitle("LEGv8 Simulator Main");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);   // Handle exit manually
        setSize(500, 350);
        setLocationRelativeTo(null);    // Center the window
        setLayout(new BorderLayout(10, 10));

        // Set custom fonts for title and buttons
        try {
            titleFont = Font.createFont(Font.TRUETYPE_FONT, new File("./resources/fonts/roboto-bold.ttf")).deriveFont(Font.BOLD, 20);
        } catch (Exception e) {
            System.err.println(ColoredLog.WARNING + "Could not load custom font. Using default.");
            titleFont = new Font("Monospaced", Font.BOLD, 25);
        }
        try {
            buttonFont = Font.createFont(Font.TRUETYPE_FONT, new File("./resources/fonts/roboto-regular.ttf")).deriveFont(Font.PLAIN, 16);
        } catch (Exception e) {
            System.err.println(ColoredLog.WARNING + "Could not load custom font. Using default.");
            buttonFont = new Font("Monospaced", Font.PLAIN, 16);
        }
    
        // Initialize core logic objects
        configLoader = new InstructionConfigLoader();
        fileChooser = new JFileChooser("."); 

        initComponents();
        layoutComponents();

        // Handle window close event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        System.out.println(ColoredLog.SUCCESS + "Application initialized.");
    }


    // --- Initialization ---
    /**
     * Initializes the graphical user interface components, loads default configuration,
     * and sets up action listeners.
     */
    private void initComponents() {
        // Load default configuration
        configLoader.loadConfig("./resources/config/instructions_config.csv"); 
        InstructionFactory.initialize(configLoader); 

        lblStatus = new JLabel("Status: Load Assembly or change Config", SwingConstants.CENTER);
        lblStatus.setBorder(BorderFactory.createEtchedBorder());

        // Create buttons
        btnLoadConfig = new JButton("Load Instruction Config...");
        btnEditAssembly = new JButton("Edit Assembly..."); 
        btnStartSimulation = new JButton("Start Simulation");
        btnExit = new JButton("Exit Application");

        // Assign ActionListeners
        btnLoadConfig.addActionListener(this);
        btnEditAssembly.addActionListener(this);
        btnStartSimulation.addActionListener(this);
        btnExit.addActionListener(this);
        
        // Add tooltips
        btnLoadConfig.setToolTipText("Load the .csv file defining instruction formats and signals");
        btnEditAssembly.setToolTipText("Open the built-in assembly code editor");
        btnStartSimulation.setToolTipText("Open the simulation window (requires Config and Assembly)");
    }

    /**
     * Arranges the GUI components within the main application window using BorderLayout and GridLayout.
     */
    private void layoutComponents() {
        // Title label
        JLabel titleLabel = new JLabel("LEGv8 Simulator", SwingConstants.CENTER);
        titleLabel.setFont(titleFont);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 20, 20)); 
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40)); 
        buttonPanel.add(btnLoadConfig);
        buttonPanel.add(btnEditAssembly); 
        buttonPanel.add(btnStartSimulation);
        buttonPanel.add(btnExit);
        
        // Set layout
        add(lblStatus, BorderLayout.SOUTH);
        add(titlePanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
    }


    // --- Event Handling ---

    /**
     * Handles action events triggered by the main application buttons
     * (Load Config, Edit Assembly, Start Simulation, Exit).
     * @param e The ActionEvent object describing the event source and action.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == btnLoadConfig) {
            loadInstructionConfig();
        } else if (source == btnEditAssembly) {
            openAssemblyEditor();
        } else if (source == btnStartSimulation) {
            startSimulation();
        } else if (source == btnExit) {
            handleExit();
        }
    }


    // --- Core Action Methods ---

    /**
     * Opens a file chooser dialog for the user to select and load an
     * instruction configuration CSV file. Initializes the InstructionFactory
     * with the loaded configuration upon success.
     */
    private void loadInstructionConfig() {
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Config Files (*.csv)", "csv");
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("Select Instruction Configuration File");

        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File configFile = fileChooser.getSelectedFile();
            System.out.println(ColoredLog.INFO + "Selected config file: " + configFile.getAbsolutePath());
            String resourcePath = configFile.getAbsolutePath();
            
            // Attempt to load config using the file path method
            if (configLoader.loadConfig(resourcePath)) {
                try {
                    InstructionFactory.initialize(configLoader); 
                    
                    lblStatus.setText("Status: Config loaded. Load Assembly.");
                    System.out.println(ColoredLog.SUCCESS + "InstructionFactory initialized successfully.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error initializing InstructionFactory:\n" + ex.getMessage(), "Factory Init Error", JOptionPane.ERROR_MESSAGE);
                    lblStatus.setText("Status: Config loaded, but Factory init failed.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Failed to load Config from " + configFile.getName() + "\nCheck console for details.", "Config Error", JOptionPane.ERROR_MESSAGE);
                lblStatus.setText("Status: Error loading Instruction Config.");
            }
            
        }
    }

    /**
     * Opens (or brings to the front) the AssemblyEditor window.
     * Ensures the InstructionFactory is initialized before opening.
     */
    private void openAssemblyEditor() {
        if (assemblyEditor == null) { 
            assemblyEditor = new AssemblyEditor(); 
        }

        assemblyEditor.setVisible(true);
        assemblyEditor.toFront();
    }

    /**
     * Retrieves the list of successfully assembled instructions from the
     * AssemblyEditor instance. Displays warnings if the editor or instructions
     * are not available.
     * @return true if instructions were successfully retrieved, false otherwise.
     */
    private boolean loadInstructionsFromEditor() {
        if (assemblyEditor == null) {
            JOptionPane.showMessageDialog(this, "Please open the Assembly Editor first.", "Editor Required", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (assemblyEditor.getAssembledInstructions().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No instructions to load from the editor.", "No Instructions", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        loadedInstructions = assemblyEditor.getAssembledInstructions();
        
        lblStatus.setText("Status: Assembly loaded from editor.");
        System.out.println(ColoredLog.INFO + "Loaded instructions from editor: " + loadedInstructions);
        return true;    
    }

    /**
     * Coordinates the process of starting the simulation:
     * 1. Ensures configuration is loaded.
     * 2. Ensures the Assembly Editor is open.
     * 3. Loads assembled instructions from the editor.
     * 4. Initializes or updates the SimulatorEngine.
     * 5. Creates or updates and displays the SimulationView window.
     */
    private void startSimulation() {
        if (!loadInstructionsFromEditor()) {
            System.err.println(ColoredLog.FAILURE + "Failed to load instructions from editor.");
            return;
        } 

        simulatorEngine = new SimulatorEngine(configLoader, new InstructionMemory());
        simulatorEngine.loadInstructions(loadedInstructions); 

        simulationView = new SimulationView(simulatorEngine);
        simulationView.setVisible(true);
    }

    /**
     * Handles the application exit request. Prompts the user for confirmation
     * and disposes of child windows before terminating the application.
     */
    private void handleExit() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to exit the LEGv8 Simulator?",
            "Confirm Exit",
            JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            if (assemblyEditor != null) assemblyEditor.dispose();
            System.exit(0);
        }
    }

    // --- Main Method ---
    /**
     * Main entry point for the LEGv8 Simulator application.
     * Sets the system look and feel and launches the Application window on the EDT.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println(ColoredLog.WARNING + "Could not set system look and feel.");
        }
        
        SwingUtilities.invokeLater(() -> {
            Application app = new Application();
            app.setVisible(true);
        });
    }
}