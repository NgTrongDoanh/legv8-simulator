/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.gui;

import legv8.exceptions.*;
import legv8.simulator.*;
import legv8.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * SimulationView is a GUI component that provides a visual representation of the LEGv8 simulator.
 * It allows users to control the simulation, view register and memory states, and step through instructions.
 * The view includes buttons for running, pausing, stepping through instructions, and resetting the simulation.
 * It also provides sliders for adjusting simulation speed and buttons for showing/hiding register, memory, and instruction views.
 */
public class SimulationView extends JFrame implements ActionListener { 

    // --- Engine for simulation ---
    private SimulatorEngine simulatorEngine;
    
    // --- GUI Components ---
    private DatapathCanvas datapathCanvas; 
    
    // Control buttons and sliders
    private JButton btnRun, btnPause;
    private JButton btnStepByStep, btnResetIns;
    private boolean isStep = false;

    private JButton btnNextIns, btnResetProgram;
    private JButton btnShowRegisters, btnShowMemory, btnShowInstructions;
    private JButton btnCloseView;
    
    private JPanel controlPanel; 
    private JPanel datapathPanel; 
    private JLabel lblStatus;

    // Views for registers, memory, and instructions    
    private RegisterView registerView;
    private MemoryView memoryView;
    private InstructionView instructionView;

    // Simulation state variables
    private boolean isPaused = true;
    private final Timer simulationTimer;
    private int simulationDelayMs = 500; 

    // Microsteps for simulation
    private List<MicroStep> microSteps; 
    private int currentMicroStepIndex = -1; 


    // --- Constructor ---

    /**
     * Constructor for SimulationView.
     * @param engine The SimulatorEngine to be used for simulation.
     */
    public SimulationView(SimulatorEngine engine) {
        setTitle("LEGv8 Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1800, 1024); 
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        initComponents();
        layoutComponents();

        this.simulatorEngine = engine;

        simulationTimer = new Timer(simulationDelayMs, e -> stepExecution());    
        simulationTimer.setInitialDelay(simulationDelayMs);
        simulationTimer.setDelay(simulationDelayMs);
    }


    // --- Helper Methods for constructing the GUI ---

    /**
     * Initializes the GUI components for the simulation view.
     */
    private void initComponents() {        
        // Initialize buttons and labels for controls
        btnRun = new JButton("Run");
        btnPause = new JButton("Pause");
        btnStepByStep = new JButton("Step By Step");
        btnResetIns = new JButton("Reset Step");
        btnNextIns = new JButton("Next Instruction");
        btnResetProgram = new JButton("Reset Program");
        
        // Initialize buttons for showing/hiding views
        btnShowRegisters = new JButton("Registers");
        btnShowMemory = new JButton("Data Memory");
        btnShowInstructions = new JButton("Instructions");
        btnCloseView = new JButton("Close Simulation");
        
        // Initialize status label
        lblStatus = new JLabel("Status: Idle", JLabel.LEFT);
        lblStatus.setBorder(BorderFactory.createEtchedBorder());

        // Initialize datapath canvas and panel
        datapathCanvas = new DatapathCanvas(); 
        datapathPanel = new JPanel(new BorderLayout()); 
        datapathPanel.add(datapathCanvas, BorderLayout.CENTER); 
        datapathPanel.setBackground(Color.DARK_GRAY); 
        datapathPanel.setBorder(BorderFactory.createLoweredBevelBorder());

        // Add action listeners to buttons
        btnRun.addActionListener(this);
        btnPause.addActionListener(this);
        btnStepByStep.addActionListener(this);
        btnResetIns.addActionListener(this);
        btnNextIns.addActionListener(this);
        btnResetProgram.addActionListener(this);
        // speedSlider.addChangeListener(this);
        btnShowRegisters.addActionListener(this);
        btnShowMemory.addActionListener(this);
        btnShowInstructions.addActionListener(this);
        btnCloseView.addActionListener(this);

        // Set tooltips for buttons
        btnRun.setToolTipText("Run the simulation continuously");
        btnPause.setToolTipText("Pause or resume continuous execution");
        btnStepByStep.setToolTipText("Execute one full instruction cycle");
        btnResetIns.setToolTipText("Reset the Instruction Step");
        btnNextIns.setToolTipText("Execute the next instruction in the program");
        btnResetProgram.setToolTipText("Reset the program counter to the start");
        btnShowRegisters.setToolTipText("Show/Hide the Register View window");
        btnShowMemory.setToolTipText("Show/Hide the Data Memory View window");
        btnShowInstructions.setToolTipText("Show/Hide the Instruction Memory View window");
        btnCloseView.setToolTipText("Close this simulation window");
    }

    /**
     * Sets the layout for the components in the simulation view.
     * The layout consists of a datapath panel, control panel, and status label.
     */
    private void layoutComponents() { 
        controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Controls"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE; 
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5); 
        
        JPanel execPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        execPanel.add(btnRun);
        execPanel.add(btnPause);
        controlPanel.add(execPanel, gbc);
        
        JPanel stepPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        stepPanel.add(btnStepByStep);
        stepPanel.add(btnResetIns);
        controlPanel.add(stepPanel, gbc);

        JPanel instructionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        instructionPanel.add(btnNextIns);
        instructionPanel.add(btnResetProgram);
        controlPanel.add(instructionPanel, gbc); 
        
        gbc.insets = new Insets(10, 0, 10, 0); 
        controlPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
        gbc.insets = new Insets(2, 5, 2, 5); 
        
        controlPanel.add(btnShowRegisters, gbc);
        controlPanel.add(btnShowMemory, gbc);
        controlPanel.add(btnShowInstructions, gbc);
        
        gbc.insets = new Insets(10, 0, 10, 0);
        controlPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
        gbc.insets = new Insets(2, 5, 2, 5);
        
        gbc.insets = new Insets(10, 0, 10, 0);
        controlPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
        gbc.insets = new Insets(2, 5, 2, 5);
        
        gbc.weighty = 1.0; 
        gbc.anchor = GridBagConstraints.SOUTH; 
        controlPanel.add(btnCloseView, gbc);
        
        setLayout(new BorderLayout(5, 5));
        add(datapathPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);
        add(lblStatus, BorderLayout.SOUTH);     
    }

    private void updateStateViews() {
        if (microSteps == null) return;
        int idx = currentMicroStepIndex;
        if (microSteps.isEmpty() || currentMicroStepIndex >= microSteps.size()) {
            return;
        }

        if (idx < 0) {
            idx = 0;
        }

        MicroStep currentStep = microSteps.get(idx);

        if (currentMicroStepIndex >= 0 && datapathCanvas != null) {
            datapathCanvas.updateState(currentStep);
        }

        if (registerView != null) {
            registerView.updateData(currentStep.registerStorage(), -1);
        }

        if (memoryView != null) {
            memoryView.updateData(currentStep.memoryStorage(), -1L);
        }

        if (instructionView != null) {
            instructionView.updateData(simulatorEngine.getInstructionMemory(), currentStep.programCounter());
        }
    }
    // --- ActionListener and ChangeListener methods ---

    /**
     * Handles button clicks and slider changes in the simulation view.
     * @param e The ActionEvent or ChangeEvent triggered by user interaction.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnRun) {
            startSimulation();
        } else if (src == btnPause) {
            if (isPaused) resumeSimulation();
            else pauseSimulation();
        } else if (src == btnStepByStep) {
            stepByStepSimulation();
        } else if (src == btnResetIns) {
            resetInstructionStep();
        } else if (src == btnNextIns) {
            nextInstructionSimulation();
        } else if (src == btnResetProgram) {
            resetProgramSimulation();
        } else if (src == btnShowRegisters) {
            toggleRegisterView();
        } else if (src == btnShowMemory) {
            toggleMemoryView();
        } else if (src == btnShowInstructions) {
            toggleInstructionView();
        } else if (src == btnCloseView) {
            handleCloseView();
        }
    }


    // --- State Update Methods ---

    /**
     * Updates the state of the simulation view based on the current simulation state.
     */
    void updateDefaultButton() {
        this.isStep = false;
        this.isPaused = true;
        btnPause.setText("Pause");
        updateControlButton(true, false, false, true, true, true, true);
    }

    /**
     * Updates the state of the simulation view when the simulation is running.
     */
    void updateRunningButton() {
        this.isStep = false;
        this.isPaused = false;
        updateControlButton(false, true, false, true, true, true, true);
    }

    /**
     * Updates the state of the simulation view when the simulation is paused.
     */
    void updatePauseButton() {
        this.isStep = false;
        this.isPaused = true;
        updateControlButton(false, false, true, true, true, true, true);
    }

    /**
     * Updates the state of the simulation view when the simulation is resumed.
     */
    void updateResumeButton() {
        this.isStep = false;
        this.isPaused = false;
        updateControlButton(false, true, false, true, true, true, true);
    }

    /**
     * Updates the state of the simulation view when the simulation is in step-by-step mode.
     */
    void updateStepByStepButton() {
        this.isStep = true;
        this.isPaused = false;
        updateControlButton(true, false, false, true, true, true, true);
    }

    /**
     * Updates the state of the simulation view when the instruction step is reset.
     */
    void updateResetStepButton() {
        this.isStep = false;
        this.isPaused = false;
        updateControlButton(true, false, false, true, true, true, true);
    }

    /**
     * Updates the state of the simulation view when the next instruction is executed.
     */
    void updateNextInstructionButton() {
        this.isStep = false;
        this.isPaused = false;
        updateControlButton(true, false, false, true, true, true, true);
    }

    /**
     * Updates the state of the simulation view when the program is reset.
     */
    void updateResetProgramButton() {
        this.isStep = false;
        this.isPaused = false;
        updateControlButton(true, false, false, true, true, true, true);
    }

    /**
     * Updates the state of the simulation view when the simulation ends.
     */
    void updateEndRunningButton() {
        this.isStep = false;
        this.isPaused = true;
        updateControlButton(true, false, false, true, true, true, true);
    }

    /**
     * Updates the control state of the simulation view based on the provided parameters.
     * @param canRun Indicates if the Run button should be enabled.
     * @param canPause Indicates if the Pause button should be enabled.
     * @param canResume Indicates if the Resume button should be enabled.
     * @param canStepByStep Indicates if the Step By Step button should be enabled.
     * @param canResetStep Indicates if the Reset Step button should be enabled.
     * @param canNextIns Indicates if the Next Instruction button should be enabled.
     * @param canResetProgram Indicates if the Reset Program button should be enabled.
     */
    private void updateControlButton(boolean canRun, boolean canPause, boolean canResume, boolean canStepByStep, boolean canResetStep, boolean canNextIns, boolean canResetProgram) {
        btnRun.setEnabled(canRun);
        btnPause.setEnabled(canPause || canResume); 
        btnPause.setText(isPaused ? "Resume" : "Pause");
        
        btnStepByStep.setEnabled(canStepByStep);
        btnResetIns.setEnabled(canResetStep);
        btnNextIns.setEnabled(canNextIns);
        btnResetProgram.setEnabled(canResetProgram);

        if (isPaused) {
            btnPause.setToolTipText(canResume ? "Resume continuous execution" : "Cannot resume (Halted or no engine)");
        } else {
            btnPause.setToolTipText(canPause ? "Pause continuous execution" : ""); 
        }
    }


    // --- Handle Methods ---

    /**
     * Handles the closing of the simulation view.
     * Stops the simulation timer and disposes of any open views.
     */
    private void handleCloseView() {
        simulationTimer.stop(); 
        
        if (registerView != null) registerView.dispose();
        if (memoryView != null) memoryView.dispose();
        if (instructionView != null) instructionView.dispose();
        
        setVisible(false);
    }

    private void stepExecution() {
        currentMicroStepIndex++;
        if (currentMicroStepIndex >= microSteps.size()) {
            simulationTimer.stop();
            lblStatus.setText("Status: Completed");
        } else {
            updateStateViews();
        }
}

    /**
     * Starts the simulation by executing microsteps.
     * If the simulation is already running or halted, it shows an error message.
     */
    private void startSimulation() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (simulationTimer.isRunning()) {
            JOptionPane.showMessageDialog(this, "Simulation is already running.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        lblStatus.setText("Status: Running");
        updateRunningButton();

        simulationTimer.start();
        simulationTimer.setInitialDelay(simulationDelayMs);
        simulationTimer.setDelay(simulationDelayMs);
        
        try {
            if (microSteps == null) {
                microSteps = simulatorEngine.getMicroSteps();
            }
        } catch (SimulationException e) {
            JOptionPane.showMessageDialog(this, "Error during simulation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            simulationTimer.stop(); 
            lblStatus.setText("Status: Paused");
            updateResetStepButton();
            return;
        }
    }

    /**
     * Pauses the simulation and updates the status label.
     * If the simulation is already paused, it shows an error message (Just for unexpected bugs).
     */
    private void pauseSimulation() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        updatePauseButton();

        simulationTimer.stop();
        lblStatus.setText("Status: Paused");

        // updateStateViews();
    }

    /**
     * Resumes the simulation from a paused state.
     * If the simulation is not paused, it shows an error message (Just for unexpected bugs).
     */
    private void resumeSimulation() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        updateResumeButton();

        simulationTimer.start(); 
        simulationTimer.setInitialDelay(simulationDelayMs);
        simulationTimer.setDelay(simulationDelayMs);
        
        lblStatus.setText("Status: Resuming...");
    }

    /**
     * Executes a single microstep in the simulation.
     * If the simulation is not running, it shows an error message.
     */
    private void stepByStepSimulation() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            if (microSteps == null) {
                microSteps = simulatorEngine.getMicroSteps();
            }
        } catch (SimulationException e) {
            JOptionPane.showMessageDialog(this, "Error during simulation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (simulationTimer.isRunning()) {
            simulationTimer.stop(); 
        }

        if (!isStep) {
            updateStepByStepButton();
        } 

        currentMicroStepIndex++;

        System.out.println(ColoredLog.START_PROCESS + "Executing microstep: " + (currentMicroStepIndex) + " / " + microSteps.size());
        updateStateViews();
    }

    /**
     * Resets the instruction step in the simulation.
     * If the simulation engine is not available, it shows an error message.
     */
    void resetInstructionStep() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        
        lblStatus.setText("Status: Instruction Step Reset");
        
        if (simulationTimer.isRunning()) {
            simulationTimer.stop(); 
        }

        microSteps.clear();
        microSteps = simulatorEngine.getMicroStepsWithoutStep(); 
        currentMicroStepIndex = -1; 
        
        if (datapathCanvas != null) {
            datapathCanvas.resetState(); 
        }
        
        updateResetStepButton();
        updateStateViews();
    }

    /**
     * Executes the next instruction in the simulation.
     * If the simulation engine is not available, it shows an error message.
     */
    void nextInstructionSimulation() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (simulationTimer.isRunning()) {
            simulationTimer.stop(); 
        }
        
        try {
            if (microSteps != null) microSteps.clear();
            microSteps = simulatorEngine.getMicroSteps();
        } catch (SimulationException e) {
            JOptionPane.showMessageDialog(this, "Error during simulation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (datapathCanvas != null) {
            datapathCanvas.resetState(); 
        }
        
        currentMicroStepIndex = -1;
        updateNextInstructionButton();
        updateStateViews();
    }

    /**
     * Resets the program simulation by resetting the program counter and updating the status label.
     * If the simulation engine is not available, it shows an error message.
     */
    void resetProgramSimulation() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (simulationTimer.isRunning()) {
            simulationTimer.stop(); 
        }

        simulatorEngine.reset();
        microSteps.clear();
        try {
            microSteps = simulatorEngine.getMicroSteps();
        } catch (SimulationException e) {
            JOptionPane.showMessageDialog(this, "Error during simulation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentMicroStepIndex = -1; 
        if (datapathCanvas != null) {
            datapathCanvas.resetState(); 
        }
        
        updateResetProgramButton();
        updateStateViews();
        lblStatus.setText("Status: Program Counter Reset");
    }

    // --- View Toggle Methods ---

    private void toggleRegisterView() {
        if (registerView == null) {
            registerView = new RegisterView(this, simulatorEngine.getRegisterController().getStorage());
            registerView.updateData(simulatorEngine.getRegisterController().getStorage(), -1);
        }

        registerView.setVisible(!registerView.isVisible());
        if (registerView.isVisible()) registerView.toFront();
    }

    private void toggleMemoryView() {
        if (memoryView == null) {
            memoryView = new MemoryView(this, simulatorEngine.getDataMemoryController().getStorage());
            memoryView.updateData(simulatorEngine.getDataMemoryController().getStorage(), -1L);
        }
       
        memoryView.setVisible(!memoryView.isVisible());
        if (memoryView.isVisible()) memoryView.toFront();
    }

    private void toggleInstructionView() {
        if (instructionView == null) {
            instructionView = new InstructionView(this, simulatorEngine.getInstructionMemory());
            instructionView.updateData(simulatorEngine.getInstructionMemory(), simulatorEngine.getProgramCounter().getCurrentAddress());
        }
       
        instructionView.setVisible(!instructionView.isVisible());
        if (instructionView.isVisible()) {
            instructionView.toFront();
            instructionView.highlightPCRow(simulatorEngine.getProgramCounter().getCurrentAddress());
        }
    }

    // --- Helper Methods for Error Handling ---
    
    /**
     * Displays an error message dialog with the specified message.
     * @param message The error message to display.
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Simulation Error", JOptionPane.ERROR_MESSAGE);
        lblStatus.setText("Status: Error");
    }

}