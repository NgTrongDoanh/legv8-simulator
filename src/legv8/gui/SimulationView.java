package legv8.gui;

import legv8.assembler.*;
import legv8.core.*;
import legv8.datapath.*;
import legv8.exceptions.*;
import legv8.instructions.*;
import legv8.simulator.*;
import legv8.storage.*;
import legv8.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimulationView extends JFrame implements ActionListener, ChangeListener { 

    private SimulatorEngine simulatorEngine;
    
    private DatapathCanvas datapathCanvas; 

    
    private JButton btnRun, btnPause;
    private JButton btnStepByStep, btnResetIns;
    static boolean isStep = false;

    private JButton btnNextIns, btnResetProgram;
    private JSlider speedSlider;
    private JLabel lblSpeed;
    private JButton btnShowRegisters, btnShowMemory, btnShowInstructions;
    private JButton btnCloseView;
    
    private JPanel controlPanel; 
    private JPanel datapathPanel; 
    private JLabel lblStatus;

    
    private RegisterView registerView;
    private MemoryView memoryView;
    private InstructionView instructionView;

    
    private boolean isPaused = true;
    
    private Timer simulationTimer;
    private int simulationDelayMs = 200; 

    
    private InstructionMemory instructionMemoryRef;
    private RegisterFileController registerControllerRef;
    private DataMemoryController memoryControllerRef;
    private ProgramCounter pcRef;

    private List<MicroStep> microSteps; 
    private int currentMicroStepIndex = -1; 


    public SimulationView(SimulatorEngine engine) {
        setTitle("LEGv8 Simulator");
        setSize(1800, 1024); 
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();

        simulationTimer = new Timer(simulationDelayMs, e -> {});    
        simulationTimer.setInitialDelay(simulationDelayMs);
        simulationTimer.setDelay(simulationDelayMs);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleCloseView();
            }
        });

        setSimulatorEngine(engine); 
    }

    private void initComponents() {
        
        btnRun = new JButton("Run");
        btnPause = new JButton("Pause");
        
        btnStepByStep = new JButton("Step By Step");
        btnResetIns = new JButton("Reset Step");
        btnNextIns = new JButton("Next Instruction");
        btnResetProgram = new JButton("Reset Program");

        
        speedSlider = new JSlider(JSlider.HORIZONTAL, 0, 500, 500 - simulationDelayMs); 
        speedSlider.setMajorTickSpacing(100);
        speedSlider.setMinorTickSpacing(25);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true); 
        speedSlider.setToolTipText("Simulation Speed (Slower <-> Faster)");
        lblSpeed = new JLabel("Speed:", JLabel.RIGHT);

        
        btnShowRegisters = new JButton("Registers");
        btnShowMemory = new JButton("Data Memory");
        btnShowInstructions = new JButton("Instructions");
        btnCloseView = new JButton("Close Simulation");

        
        lblStatus = new JLabel("Status: Idle", JLabel.LEFT);
        lblStatus.setBorder(BorderFactory.createEtchedBorder());

        
        datapathCanvas = new DatapathCanvas(); 
        datapathPanel = new JPanel(new BorderLayout()); 
        datapathPanel.add(datapathCanvas, BorderLayout.CENTER); 
        datapathPanel.setBackground(Color.DARK_GRAY); 
        datapathPanel.setBorder(BorderFactory.createLoweredBevelBorder());




        
        btnRun.addActionListener(this);
        btnPause.addActionListener(this);
        
        btnStepByStep.addActionListener(this);
        btnResetIns.addActionListener(this);
        btnNextIns.addActionListener(this);
        btnResetProgram.addActionListener(this);

        speedSlider.addChangeListener(this);
        btnShowRegisters.addActionListener(this);
        btnShowMemory.addActionListener(this);
        btnShowInstructions.addActionListener(this);
        btnCloseView.addActionListener(this);

        
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

        
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        speedPanel.add(lblSpeed); 
        controlPanel.add(speedSlider, gbc);

        
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

    public void setSimulatorEngine(SimulatorEngine engine) {
        this.simulatorEngine = Objects.requireNonNull(engine, "SimulatorEngine cannot be null.");
        
        setComponents(
            engine.getInstructionMemory(),
            engine.getRegisterController(),
            engine.getDataMemoryController(),
            engine.getProgramCounter()
        );

        
        if (datapathCanvas != null) {
            datapathCanvas.setSimulationSpeedDelay(this.simulationDelayMs);
        }

        resetSimulationState(); 
        System.out.println(ColoredLog.SUCCESS + "SimulationView: Engine and components set.");
    }

    private void setComponents(InstructionMemory iMem, RegisterFileController rfc,
                            DataMemoryController dmc, ProgramCounter pc) {
        this.instructionMemoryRef = iMem;
        this.registerControllerRef = rfc;
        this.memoryControllerRef = dmc;
        this.pcRef = pc;
    }

    public void resetSimulationState() {
        simulationTimer.stop();
        isPaused = true;
        lblStatus.setText(simulatorEngine != null ? "Status: Reset / Idle" : "Status: No Engine");
        
        updateAllStateViews(); 

        updateDefaultState();

        currentMicroStepIndex = -1;

        
        if (datapathCanvas != null) {
            datapathCanvas.resetState();
        }
    }

    private void updateAllStateViews() {
        if (simulatorEngine == null || microSteps == null) return;
        if (microSteps.isEmpty() || currentMicroStepIndex < 0 || currentMicroStepIndex >= microSteps.size()) {
            
            
            
            return;
        }

        
        MicroStep currentMicroStep = microSteps.get(currentMicroStepIndex);
        
        
        Set<ComponentID> activeComponents = new HashSet<>();
        Set<BusID> activeBuses = new HashSet<>();

        if (currentMicroStep != null && currentMicroStep.stepInfo() != null) {
            
            for (StepInfo info : currentMicroStep.stepInfo()) {
                if (info.startComponent() != null) {
                    activeComponents.add(info.startComponent());
                }
                if (info.endComponent() != null) {
                    activeComponents.add(info.endComponent());
                }
                if (info.bus() != null) {
                    activeBuses.add(info.bus());
                }
            }
             
             
        }
        
        
        
        if (registerView != null && registerView.isVisible()) {
            RegisterStorage regStorage = (currentMicroStep != null) ? currentMicroStep.registerStorage() : registerControllerRef.getStorage(); 
            int lastChangedReg = -1; 
            registerView.updateData(regStorage, lastChangedReg);
        }

        
        if (memoryView != null && memoryView.isVisible()) {
            MemoryStorage memStorage = (currentMicroStep != null) ? currentMicroStep.memoryStorage() : memoryControllerRef.getStorage();
            long lastChangedAddr = -1L; 
            memoryView.updateData(memStorage, lastChangedAddr); 
        }

        
        if (instructionView != null && instructionView.isVisible()) {
            long pcAddr = (currentMicroStep != null) ? currentMicroStep.programCounter() : pcRef.getCurrentAddress();
            instructionView.updateData(simulatorEngine.getInstructionMemory(), pcAddr); 
        }

        
        if (datapathCanvas != null) {
            datapathCanvas.updateState(currentMicroStep); 
        }
    }

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

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == speedSlider && !speedSlider.getValueIsAdjusting()) {
            
            int sliderValue = speedSlider.getValue();
            
            int maxDelay = 1001; 
            int minDelay = 1;   
            simulationDelayMs = maxDelay - (sliderValue * (maxDelay - minDelay) / 500);
            simulationDelayMs = Math.max(minDelay, simulationDelayMs); 

            
            

            if (datapathCanvas != null) {
                datapathCanvas.setSimulationSpeedDelay(simulationDelayMs);
            }
            System.out.println(ColoredLog.SUCCESS + "Simulation Delay set to: " + simulationDelayMs + " ms (Slider: " + sliderValue + ")");
        }
    }

    void updateDefaultState() {
        this.isStep = false;
        this.isPaused = true;
        btnPause.setText("Pause");
        updateControlState(true, false, false, true, true, true, true);
    }

    void updateRuningState() {
        this.isStep = false;
        this.isPaused = false;
        updateControlState(false, true, false, true, true, true, true);
    }

    void updatePauseState() {
        this.isStep = false;
        this.isPaused = true;
        updateControlState(false, false, true, true, true, true, true);
    }

    void updateResumeState() {
        this.isStep = false;
        this.isPaused = false;
        updateControlState(false, true, false, true, true, true, true);
    }

    void updateStepByStepState() {
        this.isStep = true;
        this.isPaused = false;
        updateControlState(true, false, false, true, true, true, true);
    }

    void updateResetStepState() {
        this.isStep = false;
        this.isPaused = false;
        updateControlState(true, false, false, true, true, true, true);
    }

    void updateNextInstructionState() {
        this.isStep = false;
        this.isPaused = false;
        updateControlState(true, false, false, true, true, true, true);
    }

    void updateResetProgramState() {
        this.isStep = false;
        this.isPaused = false;
        updateControlState(true, false, false, true, true, true, true);
    }

    void updateEndRunningState() {
        this.isStep = false;
        this.isPaused = true;
        updateControlState(true, false, false, true, true, true, true);
    }

    private void updateControlState(boolean canRun, boolean canPause, boolean canResume, boolean canStepByStep, boolean canResetStep, boolean canNextIns, boolean canResetProgram) {
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

    private void handleCloseView() {
        simulationTimer.stop(); 
        
        if (registerView != null) registerView.dispose();
        if (memoryView != null) memoryView.dispose();
        if (instructionView != null) instructionView.dispose();
        
        setVisible(false);
    }

    private void startSimulation() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (simulationTimer.isRunning()) {
            JOptionPane.showMessageDialog(this, "Simulation is already running.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (simulatorEngine.isHalted()) {
            JOptionPane.showMessageDialog(this, "Simulation is halted. Please reset to continue.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        isPaused = false;
        lblStatus.setText("Status: Running");
        updateRuningState();

        simulationTimer.start(); 
        try {
            if (microSteps == null) {
                microSteps = simulatorEngine.stepAndGetMicroSteps();
            } 
            
            
            
            
            simulationTimer.addActionListener(e -> {
                currentMicroStepIndex++;
                if (currentMicroStepIndex > microSteps.size() - 1) {
                    
                    
                    simulationTimer.stop();
                    System.err.println(ColoredLog.END_PROCESS + "Simulation completed.");
                } else {
                    System.err.println(ColoredLog.START_PROCESS + "Executing microstep: " + (currentMicroStepIndex) + " / " + microSteps.size());
                    System.out.println(ColoredLog.START_PROCESS + "Executing microstep: " + (currentMicroStepIndex) + " / " + microSteps.size());
                    System.out.println(microSteps.get(currentMicroStepIndex).toString());
                    
                    updateAllStateViews(); 
                }
            });
        } catch (SimulationException e) {
            JOptionPane.showMessageDialog(this, "Error during simulation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            simulationTimer.stop(); 
            lblStatus.setText("Status: Paused");
            updateResetStepState();
            return;
        }
    }
    
    private void pauseSimulation() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        updatePauseState();
        
        simulationTimer.stop(); 
        lblStatus.setText("Status: Paused");

        updateAllStateViews();
    }

    private void resumeSimulation() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        updateResumeState();

        simulationTimer.start(); 
        lblStatus.setText("Status: Resuming...");

        updateAllStateViews();
    }

    private void stepByStepSimulation() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            if (microSteps == null) {
                microSteps = simulatorEngine.stepAndGetMicroSteps();
            }
        } catch (SimulationException e) {
            JOptionPane.showMessageDialog(this, "Error during simulation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (simulationTimer.isRunning()) {
            simulationTimer.stop(); 
        }

        if (!isStep) {
            updateStepByStepState();
        } else {
            if (currentMicroStepIndex >= microSteps.size() - 1) {
                resetSimulationState();
            }
        }
        currentMicroStepIndex++;

        System.out.println(ColoredLog.START_PROCESS + "Executing microstep: " + (currentMicroStepIndex) + " / " + microSteps.size());
        System.out.println(ColoredLog.INFO + microSteps.get(currentMicroStepIndex).toString());
        updateAllStateViews();
    }

    void resetInstructionStep() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        resetSimulationState();
        lblStatus.setText("Status: Instruction Step Reset");
        updateAllStateViews();
    }

    void nextInstructionSimulation() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            if (microSteps != null) microSteps.clear();
            microSteps = simulatorEngine.stepAndGetMicroSteps();
        } catch (SimulationException e) {
            JOptionPane.showMessageDialog(this, "Error during simulation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        resetSimulationState();
        updateNextInstructionState();
    }

    void resetProgramSimulation() {
        if (simulatorEngine == null) {
            JOptionPane.showMessageDialog(this, "No simulator engine available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        simulatorEngine.reset();
        if (microSteps != null) microSteps.clear();
        try {
            microSteps = simulatorEngine.stepAndGetMicroSteps();
        } catch (SimulationException e) {
            JOptionPane.showMessageDialog(this, "Error during simulation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        lblStatus.setText("Status: Program Counter Reset");
        updateResetProgramState();

        resetSimulationState();
    }

    

    private void toggleRegisterView() {
        if (registerControllerRef == null) { showError("Register data not available."); return; }
        if (registerView == null) {
            registerView = new RegisterView(this, registerControllerRef.getStorage());
            registerView.updateData(registerControllerRef.getStorage(), -1); 
        }
        registerView.setVisible(!registerView.isVisible());
        if (registerView.isVisible()) registerView.toFront();
        updateAllStateViews();
    }

    private void toggleMemoryView() {
    if (memoryControllerRef == null) { showError("Memory data not available."); return; }
        if (memoryView == null) {
            memoryView = new MemoryView(this, memoryControllerRef.getStorage());
            memoryView.updateData(memoryControllerRef.getStorage(), -1L); 
        }
        memoryView.setVisible(!memoryView.isVisible());
        if (memoryView.isVisible()) memoryView.toFront();
        updateAllStateViews();
    }

    private void toggleInstructionView() {
    if (instructionMemoryRef == null || pcRef == null) { showError("Instruction/PC data not available."); return; }
        if (instructionView == null) {
            instructionView = new InstructionView(this, instructionMemoryRef);
            instructionView.updateData(instructionMemoryRef, pcRef.getCurrentAddress()); 
        }
        instructionView.setVisible(!instructionView.isVisible());
        if (instructionView.isVisible()) {
            instructionView.toFront();
            instructionView.highlightPCRow(pcRef.getCurrentAddress()); 
        }
        updateAllStateViews();
    }

    

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Simulation Error", JOptionPane.ERROR_MESSAGE);
        lblStatus.setText("Status: Error");
    }
    
}