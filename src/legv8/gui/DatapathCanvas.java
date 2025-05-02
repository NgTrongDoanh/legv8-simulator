package legv8.gui;

import com.fasterxml.jackson.databind.ObjectMapper; 
import legv8.datapath.BusID;
import legv8.datapath.ComponentID;
import legv8.simulator.MicroStep;
import legv8.simulator.StepInfo;
import legv8.util.ColoredLog;

import javax.imageio.ImageIO;
import java.io.File;
import javax.swing.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;


import java.awt.event.ActionListener; 
import java.util.concurrent.ConcurrentHashMap; 

public class DatapathCanvas extends JPanel {
    private DatapathLayout layoutData; 
    
    private Map<ComponentID, BufferedImage> inactiveImages;
    private Map<ComponentID, BufferedImage> activeImages;
    private Set<ComponentID> activeComponents;
    private Set<BusID> activeBuses;

    private MicroStep currentMicroStep = null; 

    private final Color defaultBusColor = Color.BLACK;
    private final Color highlightBusColor = Color.RED; 
    private final Stroke defaultBusStroke = new BasicStroke(2); 
    private final Stroke highlightBusStroke = new BasicStroke(4); 

    private Font labelBusFont; 
    private Font labelComponentFont; 
    private Font valueFont; 
    private final int labelOffset = 5; 
    private final int ARR_SIZE = 6; 
    
    private List<DraggableLabel> draggableLabels = new ArrayList<>();

    private DraggableLabel currentlyDraggedLabel = null;
    private int dragStartXOffset, dragStartYOffset; 

    private Map<BusID, AnimationState> activeAnimations = new ConcurrentHashMap<>();
    private javax.swing.Timer animationTimer;
    private final int ANIMATION_TICK_MS = 30; 
    private long currentSimulationDelayMs = 200; 

    
    public DatapathCanvas() {
        this.inactiveImages = new HashMap<>();
        this.activeImages = new HashMap<>();
        this.activeComponents = new HashSet<>();
        this.activeBuses = new HashSet<>();
        
        try {
            this.labelBusFont = Font.createFont(Font.TRUETYPE_FONT, new File("./resources/fonts/roboto-italic.ttf")).deriveFont(12f);
        } catch (FontFormatException | IOException e) {
            System.err.println(ColoredLog.WARNING + "Error loading font: " + e.getMessage() + " - Using default font.");
            this.labelBusFont = new Font("Arial", Font.ITALIC, 12);
        }

        try {
            this.labelComponentFont = Font.createFont(Font.TRUETYPE_FONT, new File("./resources/fonts/roboto-bold.ttf")).deriveFont(16f);
        } catch (FontFormatException | IOException e) {
            System.err.println(ColoredLog.WARNING + "Error loading font: " + e.getMessage() + " - Using default font.");
            this.labelComponentFont = new Font("Arial", Font.BOLD, 16);
        }

        try {
            this.valueFont = Font.createFont(Font.TRUETYPE_FONT, new File("./resources/fonts/roboto-regular.ttf")).deriveFont(12f);
        } catch (FontFormatException | IOException e) {
            System.err.println(ColoredLog.WARNING + "Error loading font: " + e.getMessage() + " - Using default font.");
            this.valueFont = new Font("Arial", Font.PLAIN, 12);
        }

        setPreferredSize(new Dimension(1600, 900)); 
        setBackground(Color.LIGHT_GRAY); 
        
        ActionListener animationUpdater = e -> {
            boolean stillAnimating = false;
            long currentTime = System.currentTimeMillis();
       
            for (AnimationState state : activeAnimations.values()) {
                state.updateProgress(currentTime);
                if (!state.isFinished()) { 
                    stillAnimating = true; 
                }
            }
            repaint(); 
       
            if (!stillAnimating && animationTimer.isRunning()) { 
                animationTimer.stop();
            }
        };

        animationTimer = new javax.swing.Timer(ANIMATION_TICK_MS, animationUpdater);
        animationTimer.setInitialDelay(0); 
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePress(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseRelease();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDrag(e.getPoint());
            }
        });

        loadLayoutAndImages(); 
        initializeDraggableLabels();
    }
    
    private void initializeDraggableLabels() {
        draggableLabels.clear(); 
        if (layoutData == null || layoutData.components == null) return;

        for (ComponentInfo compInfo : layoutData.components) {
            if (compInfo.connectionPoints == null) continue;
            try {
                ComponentID compId = ComponentID.valueOf(compInfo.id);
                for (Map.Entry<String, ConnectionPoint> entry : compInfo.connectionPoints.entrySet()) {
                    ConnectionPoint cp = entry.getValue();
                    String cpName = entry.getKey();
                    String labelText = cp.name;

                    if (labelText != null && !labelText.isEmpty()) {
                        
                        int anchorX = cp.x;
                        int anchorY = cp.y;
                        
                        draggableLabels.add(new DraggableLabel(labelText, compId, cpName, anchorX, anchorY));
                    }
                }
            } catch (IllegalArgumentException e) {
                System.err.println(ColoredLog.FAILURE + "Error initializing labels: Invalid component ID " + compInfo.id);
            }
        }
        System.out.println(ColoredLog.SUCCESS + "Initialized " + draggableLabels.size() + " draggable labels.");
    }

    private void loadLayoutAndImages() {
        ObjectMapper mapper = new ObjectMapper(); 
        String jsonFilePath = "/resources/layout/datapath_layout.json"; 

        try (InputStream is = DatapathCanvas.class.getResourceAsStream(jsonFilePath)) {
            if (is == null) {
                System.err.println(ColoredLog.ERROR + "FATAL ERROR: Cannot find JSON file: " + jsonFilePath);
                
                return;
            }

            
            layoutData = mapper.readValue(is, DatapathLayout.class);
            System.out.println(ColoredLog.SUCCESS + "Successfully loaded layout data.");

            
            
            if (layoutData.canvasSize != null) {
                setPreferredSize(new Dimension(layoutData.canvasSize.width, layoutData.canvasSize.height));
            }

            
            loadComponentImages();
            initializeDraggableLabels();

            
            

        } catch (IOException e) {
            System.err.println(ColoredLog.ERROR + "FATAL ERROR: Failed to read or parse JSON file: " + jsonFilePath);
            e.printStackTrace();
            
        }
    }

    
    public void setSimulationSpeedDelay(int delayMs) {
        this.currentSimulationDelayMs = Math.max(ANIMATION_TICK_MS * 2, delayMs); 
        System.out.println(ColoredLog.SUCCESS + "DatapathCanvas: Simulation delay set to " + this.currentSimulationDelayMs + "ms");
    } 
    
    

    
    
    private ComponentInfo getComponentInfo(ComponentID id) {
        if (layoutData == null || layoutData.components == null) return null;
        for (ComponentInfo info : layoutData.components) {
            
            if (info.id.equals(id.name())) {
                return info;
            }
        }
        return null; 
    }

    

    private void loadComponentImages() {
        if (layoutData == null || layoutData.components == null) {
            System.err.println(ColoredLog.ERROR + "Error: Layout data not loaded, cannot load images.");
            return;
        }

        String basePath = "/resources/images/"; 

        for (ComponentInfo compInfo : layoutData.components) {
            try {
                ComponentID id = ComponentID.valueOf(compInfo.id); 

                
                String inactiveFileName = basePath + compInfo.asset + "_inactive.png";
                String activeFileName = basePath + compInfo.asset + "_active.png";

                
                InputStream isInactive = DatapathCanvas.class.getResourceAsStream(inactiveFileName);
                if (isInactive != null) {
                    inactiveImages.put(id, ImageIO.read(isInactive));
                    isInactive.close(); 
                } else {
                    System.err.println(ColoredLog.WARNING + "Warning: Cannot find inactive image: " + inactiveFileName);
                    
                }

                
                InputStream isActive = DatapathCanvas.class.getResourceAsStream(activeFileName);
                if (isActive != null) {
                    activeImages.put(id, ImageIO.read(isActive));
                    isActive.close(); 
                } else {
                    System.err.println(ColoredLog.WARNING + "Warning: Cannot find active image: " + activeFileName);
                    
                    if (inactiveImages.containsKey(id)) {
                        activeImages.put(id, inactiveImages.get(id)); 
                    }
                }

            } catch (IllegalArgumentException e) {
                System.err.println(ColoredLog.WARNING + "Error: Component ID mismatch between JSON ('" + compInfo.id + "') and Enum ComponentID.");
            } catch (IOException e) {
                System.err.println(ColoredLog.WARNING + "Error loading image for component: " + compInfo.id);
                e.printStackTrace();
            }
        }
        System.out.println(ColoredLog.SUCCESS + "Finished loading component images.");
    }

    

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 

        if (layoutData == null) {
            
            g.setColor(Color.RED);
            g.drawString("Error: Could not load datapath layout.", 20, 20);
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create(); 
        try {
            
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                  
            if (layoutData.buses != null) {
                for (BusInfo busInfo : layoutData.buses) {
                    drawBus(g2d, busInfo); 
                }
            }
                        
            g2d.setFont(labelComponentFont);            
            g2d.setColor(Color.BLACK);
            if (layoutData.components != null) {
                for (ComponentInfo compInfo : layoutData.components) {
                    drawComponent(g2d, compInfo); 
                }
            }

            g2d.setFont(labelBusFont);
            g2d.setColor(Color.DARK_GRAY);
            FontMetrics fm = g2d.getFontMetrics();
            int textHeight = fm.getHeight();
            int ascent = fm.getAscent();

            for (DraggableLabel label : draggableLabels) {
                
                ComponentInfo ownerComp = getComponentInfo(label.ownerComponentId);
                if (ownerComp == null) continue; 

                
                
                ConnectionPoint cp = ownerComp.connectionPoints.get(label.connectionPointName);
                if (cp == null) continue; 

                int defaultX, defaultY;
                String[] lines = label.originalMultiLineText.split("\n"); 
                int textWidth = 0;
                for (String line : lines) { textWidth = Math.max(textWidth, fm.stringWidth(line)); }

                if (cp.labelXOffset != null && cp.labelYOffset != null) { 
                    defaultX = label.anchorX + cp.labelXOffset;
                    defaultY = label.anchorY + cp.labelYOffset + ascent; 
                } else { 
                    int centerX = ownerComp.x + ownerComp.width / 2;
                    int centerY = ownerComp.y + ownerComp.height / 2;
                    boolean preferHorizontal = Math.abs(label.anchorX - centerX) * ownerComp.height > Math.abs(label.anchorY - centerY) * ownerComp.width;

                    if (preferHorizontal) {
                        defaultY = label.anchorY - (lines.length * textHeight / 2) + ascent;
                        if (label.anchorX < centerX) { defaultX = label.anchorX - textWidth - labelOffset; }
                        else { defaultX = label.anchorX + labelOffset; }
                    } else {
                        defaultX = label.anchorX - textWidth / 2;
                        if (label.anchorY < centerY) { defaultY = label.anchorY - (lines.length * textHeight) + ascent - labelOffset; }
                        else { defaultY = label.anchorY + ascent + labelOffset; }
                    }
                    
                    if (defaultX < 2) defaultX = 2;
                    if (defaultY < textHeight) defaultY = textHeight;
                }
                

                
                int currentX = defaultX + label.offsetX;
                int currentY = defaultY + label.offsetY;
                

                
                int drawY = currentY;
                int totalTextHeight = lines.length * textHeight;
                int actualWidth = textWidth; 

                for (String line : lines) {
                    int drawX = currentX;
                    
                    if (cp.labelXOffset == null && cp.labelYOffset == null) {
                        int centerX = ownerComp.x + ownerComp.width / 2;
                        int centerY = ownerComp.y + ownerComp.height / 2;
                        boolean preferHorizontal = Math.abs(label.anchorX - centerX) * ownerComp.height > Math.abs(label.anchorY - centerY) * ownerComp.width;
                        if(!preferHorizontal){
                            int currentLineWidth = fm.stringWidth(line);
                            drawX = label.anchorX - currentLineWidth / 2 + label.offsetX; 
                            if (drawX < 2) drawX = 2;
                            actualWidth = Math.max(actualWidth, currentLineWidth); 
                        }
                    }
                    g2d.drawString(line, drawX, drawY);
                    drawY += textHeight;
                }

                label.updateBounds(currentX, currentY - ascent, actualWidth, totalTextHeight);
            } 

            if (!activeAnimations.isEmpty()) {

                List<AnimationState> animationsToDraw = new ArrayList<>(activeAnimations.values());
                for (AnimationState state : animationsToDraw) {
                    drawAnimatedValue(g2d, state);
                }
            }

        } finally {
            g2d.dispose(); 
        }
    }

    
    private void drawComponent(Graphics2D g2d, ComponentInfo compInfo) {
        try {
            ComponentID id = ComponentID.valueOf(compInfo.id);

            
            boolean isActive = activeComponents.contains(id); 
            BufferedImage img = isActive ? activeImages.get(id) : inactiveImages.get(id); 

            
            if (isActive && img == null) {
                img = inactiveImages.get(id);
            }
            
        
            if (img != null) {
                g2d.drawImage(img, compInfo.x, compInfo.y, compInfo.width, compInfo.height, this);
            } else {
                
                g2d.setColor(isActive ? Color.YELLOW : Color.ORANGE); 
                g2d.fillRect(compInfo.x, compInfo.y, compInfo.width, compInfo.height);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(compInfo.x, compInfo.y, compInfo.width, compInfo.height);
                System.err.println(ColoredLog.WARNING + "Warning: Image not found for component " + id + (isActive ? " (active)" : " (inactive)"));
            }

            
            if (compInfo.label != null && !compInfo.label.isEmpty()) {
                FontMetrics fm = g2d.getFontMetrics();
                int textHeight = fm.getHeight();
                int ascent = fm.getAscent();
                String[] lines = compInfo.label.split("\n");
                int totalLabelHeight = lines.length * textHeight;
                int maxWidth = 0;
                for (String line : lines) {
                    maxWidth = Math.max(maxWidth, fm.stringWidth(line));
                }
    
                
                int drawStartX; 
                int drawStartY; 
                if (compInfo.labelRelativeX != null) {
                    drawStartX = compInfo.x + compInfo.labelRelativeX;
                } else {
                    drawStartX = compInfo.x + (compInfo.width - maxWidth) / 2; 
                }

                if (compInfo.labelRelativeY != null) {
                    drawStartY = compInfo.y + compInfo.labelRelativeY + ascent;    
                } else {
                    drawStartY = compInfo.y + (compInfo.height - totalLabelHeight) / 2 + ascent; 
                }
                
    
                
                int currentY = drawStartY;
                for (String line : lines) {
                    int lineX = drawStartX; 
    
                    
                    
                    if (compInfo.labelRelativeX == null) {
                        int centerX = compInfo.x + compInfo.width / 2; 
                        lineX = centerX - fm.stringWidth(line) / 2; 
                    }
    
                    g2d.drawString(line, lineX, currentY);
                    currentY += textHeight;
                }
                
    
            } 
    
        } catch (IllegalArgumentException e) {
            
            g2d.setColor(Color.RED);
            g2d.drawRect(compInfo.x, compInfo.y, compInfo.width, compInfo.height);
            g2d.drawString("Invalid ID: " + compInfo.id, compInfo.x + 5, compInfo.y + 15);
            System.err.println(ColoredLog.ERROR + "Error rendering component: Invalid ID " + compInfo.id);
        }
    }


    
    private Point getAbsolutePoint(PathPoint pathPoint) {
        if (pathPoint.x != null && pathPoint.y != null) {
            
            return new Point(pathPoint.x, pathPoint.y);
        } else if (pathPoint.component != null && pathPoint.point != null) {
            
            try {
                ComponentID compId = ComponentID.valueOf(pathPoint.component);
                ComponentInfo compInfo = getComponentInfo(compId); 
                if (compInfo != null && compInfo.connectionPoints != null) {
                    ConnectionPoint cp = compInfo.connectionPoints.get(pathPoint.point);
                    if (cp != null) {
                        
                        return new Point(cp.x, cp.y);
                    } else {
                        
                        System.err.println(ColoredLog.FAILURE + "Error in getAbsolutePoint: Connection point '" + pathPoint.point
                                         + "' not found for component '" + pathPoint.component + "'. Check JSON connectionPoints keys.");
                        return null; 
                    }
                } else {
                    
                    System.err.println(ColoredLog.FAILURE + "Error in getAbsolutePoint: Component info or connection points map not found for component ID '"
                                     + pathPoint.component + "'. Check JSON components definition.");
                    return null; 
                }
            } catch (IllegalArgumentException e) {
                
                 System.err.println(ColoredLog.FAILURE + "Error in getAbsolutePoint: Invalid component ID '" + pathPoint.component
                                  + "' found in bus path. Check JSON bus definitions and ComponentID Enum.");
                return null; 
            }
        }
            
        System.err.println(ColoredLog.WARNING + "Warning in getAbsolutePoint: Could not determine absolute coordinates for path point. JSON structure might be invalid for: " + pathPoint);
        return null; 
    }


    
    private void drawBus(Graphics2D g2d, BusInfo busInfo) {
        if (busInfo.path == null || busInfo.path.size() < 2) {
            return;
        }

        BusID id = null;
        boolean isBusHighlighted = false; 
        
        Color defaultColorFromJSON = defaultBusColor; 

        
        try {
            id = BusID.valueOf(busInfo.id);
            
            isBusHighlighted = activeBuses.contains(id);
            

            
            

            
            if (busInfo.color != null && !busInfo.color.isEmpty()) {
                try {
                    defaultColorFromJSON = Color.decode(busInfo.color);
                } catch (NumberFormatException e) {
                    System.err.println(ColoredLog.WARNING + "Warning: Invalid color format '" + busInfo.color + "' for bus '" + busInfo.id + "'. Using default.");
                }
            } else {
                System.err.println(ColoredLog.WARNING + "Warning: Missing color field for bus '" + busInfo.id + "'. Using default.");
            }

        } catch (IllegalArgumentException e) {
            System.err.println(ColoredLog.WARNING + "Warning: Bus ID mismatch JSON/Enum for '" + busInfo.id + "'.");
            isBusHighlighted = false; 
        }
        

        
        Color busLineColor = isBusHighlighted ? highlightBusColor : defaultColorFromJSON; 
        Stroke busLineStroke = isBusHighlighted ? highlightBusStroke : (busInfo.thickness > 0) ? new BasicStroke(busInfo.thickness) : defaultBusStroke; 

        g2d.setColor(busLineColor);
        g2d.setStroke(busLineStroke);
        
        
        
        Point prevPoint = null;
        Point currentPoint = null;
        for (int i = 0; i < busInfo.path.size(); i++) {
            PathPoint pathPointData = busInfo.path.get(i);
            currentPoint = getAbsolutePoint(pathPointData);
            if (currentPoint == null) { prevPoint = null; continue; }
            if (prevPoint != null) {
                g2d.drawLine(prevPoint.x, prevPoint.y, currentPoint.x, currentPoint.y);
            }
            prevPoint = currentPoint;
        }
        
        if (prevPoint != null && busInfo.path.size() >= 2) {
            Point secondLastPoint = getAbsolutePoint(busInfo.path.get(busInfo.path.size() - 2));
            if (secondLastPoint != null) {
                drawArrowHead(g2d, prevPoint, secondLastPoint, busLineColor); 
            }
        }
        
        
        
    
        
        
        
        
        
        
        
                
        
        
        
                
    
                
        
        
                
        
                
                
        
        
                
    
                
        
        
                
    
                
        
        
        
        
        
                
        
                
        
        
        
                
        
        
                
        
        
    }

    
    private void drawAnimatedValue(Graphics2D g2d, AnimationState animationState) {
        if (animationState == null || animationState.value == null || animationState.value.trim().isEmpty()) {
            return; 
        }

        
        BusInfo busInfo = getBusInfo(animationState.busId); 
        if (busInfo == null) return; 

        
        Point valuePoint = getPointOnPath(busInfo, animationState.progress);
        if (valuePoint == null) return; 

        
        g2d.setFont(valueFont);
        FontMetrics fm = g2d.getFontMetrics();
        String[] lines = animationState.value.split("\n"); 
        int textHeight = fm.getHeight();
        int ascent = fm.getAscent();
        int totalTextHeight = lines.length * textHeight;
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, fm.stringWidth(line));
        }
        

        
        final int VALUE_PADDING_X = 5;
        final int VALUE_PADDING_Y = 2;
        final int ARC_SIZE = 10;
        

        
        int rectW = maxWidth + 2 * VALUE_PADDING_X; 
        int rectH = totalTextHeight + 2 * VALUE_PADDING_Y; 
        int rectX = valuePoint.x - rectW / 2; 
        
        int rectY = valuePoint.y - rectH / 2 - 3; 
        

        
        Color originalColor = g2d.getColor();
        Stroke originalStroke = g2d.getStroke();
        

        
        g2d.setColor(new Color(230, 245, 255, 230)); 
        g2d.fillRoundRect(rectX, rectY, rectW, rectH, ARC_SIZE, ARC_SIZE);
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(rectX, rectY, rectW, rectH, ARC_SIZE, ARC_SIZE);
        

        
        g2d.setColor(Color.BLUE);
        int currentY = rectY + VALUE_PADDING_Y + ascent; 
        for (String line : lines) {
            
            int lineX = rectX + (rectW - fm.stringWidth(line)) / 2;
            g2d.drawString(line, lineX, currentY);
            currentY += textHeight; 
        }
        

        
        g2d.setColor(originalColor);
        g2d.setStroke(originalStroke);
        
    }

    
    private BusInfo getBusInfo(BusID busId) {
        if (layoutData == null || layoutData.buses == null || busId == null) return null;
        for (BusInfo info : layoutData.buses) {
            
            if (info.id.equals(busId.name())) {
                return info;
            }
        }
        System.err.println("Warning: BusInfo not found for BusID: " + busId); 
        return null; 
    }

    public void updateState(MicroStep step) {
        this.currentMicroStep = step; 
    
        
        Set<ComponentID> currentActiveComponents = new HashSet<>();
        Set<BusID> currentActiveBuses = new HashSet<>();
        Map<BusID, StepInfo> busInfoForNewAnimations = new HashMap<>(); 
    
        if (step != null && step.stepInfo() != null) {
             for (StepInfo info : step.stepInfo()) {
                if (info.startComponent() != null) currentActiveComponents.add(info.startComponent());
                if (info.endComponent() != null) currentActiveComponents.add(info.endComponent());
                if (info.bus() != null) {
                     currentActiveBuses.add(info.bus());
                     if (info.value() != null && !info.value().trim().isEmpty()) {
                          busInfoForNewAnimations.put(info.bus(), info);
                     }
                }
             }
        }
        this.activeComponents = currentActiveComponents;
        this.activeBuses = currentActiveBuses;
        
    
        
        Map<BusID, AnimationState> nextAnimationStates = new ConcurrentHashMap<>();
        long startTime = System.currentTimeMillis();
    
        
        for (Map.Entry<BusID, StepInfo> entry : busInfoForNewAnimations.entrySet()) {
            BusID busId = entry.getKey();
            StepInfo info = entry.getValue();
            AnimationState newState = new AnimationState(busId, info.value(), startTime, this.currentSimulationDelayMs);
            nextAnimationStates.put(busId, newState);
            
        }
    
        
        this.activeAnimations.forEach((oldBusId, oldState) -> {
            
            
            if (!nextAnimationStates.containsKey(oldBusId)) {
                
                nextAnimationStates.put(oldBusId, oldState);
                
            }
            
            
        });
    
        
        this.activeAnimations = nextAnimationStates;
        
    
        
        boolean needsAnimating = false;
        for (AnimationState state : this.activeAnimations.values()) {
            if (!state.isFinished()) { 
                needsAnimating = true;
                break;
            }
        }
    
        
        if (needsAnimating && !animationTimer.isRunning()) {
            
            animationTimer.start();
        }
        
        
    
        
    
        repaint(); 
    }
        


    public void resetState() {
        this.currentMicroStep = null;
        this.activeComponents.clear();
        this.activeBuses.clear();
        this.activeAnimations.clear(); 
        if (animationTimer.isRunning()) {
            animationTimer.stop(); 
        }
        repaint();
    }

    void drawArrowHead(Graphics2D g2, Point tip, Point tail, Color color) {
        double phi = Math.toRadians(20); 
        int barb = ARR_SIZE; 
    
        g2.setColor(color);
        double dy = tip.y - tail.y;
        double dx = tip.x - tail.x;
        double theta = Math.atan2(dy, dx); 
        
        double x, y, rho = theta + phi;
    
        
        for (int j = 0; j < 2; j++) {
            x = tip.x - barb * Math.cos(rho);
            y = tip.y - barb * Math.sin(rho);
            g2.drawLine(tip.x, tip.y, (int) x, (int) y);
            rho = theta - phi;
        }
    }

    private StepInfo findStepInfoForBus(BusID busId) {
        if (currentMicroStep == null || currentMicroStep.stepInfo() == null || busId == null) {
            return null;
        }
        for (StepInfo info : currentMicroStep.stepInfo()) {
            if (busId.equals(info.bus())) {
                return info; 
            }
        }
        return null; 
    }

        
    private static class DraggableLabel {
        String text;                
        String originalMultiLineText; 
        ComponentID ownerComponentId; 
        String connectionPointName; 

        
        int anchorX;
        int anchorY;

        
        int offsetX = 0;
        int offsetY = 0;

        
        Rectangle bounds = new Rectangle();

        
        boolean isDragging = false;

        DraggableLabel(String text, ComponentID owner, String cpName, int anchorX, int anchorY) {
            this.originalMultiLineText = text;
            this.text = text.replace("\n", " "); 
            this.ownerComponentId = owner;
            this.connectionPointName = cpName;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
        }

        
        void updateBounds(int x, int y, int width, int height) {
            bounds.setBounds(x, y, width, height);
        }
    }

    private void handleMousePress(Point point) {
        
        for (int i = draggableLabels.size() - 1; i >= 0; i--) {
            DraggableLabel label = draggableLabels.get(i);
            if (label.bounds.contains(point)) {
                currentlyDraggedLabel = label;
                
                dragStartXOffset = point.x - label.bounds.x;
                dragStartYOffset = point.y - label.bounds.y;
                
                label.isDragging = true;
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)); 
                repaint(); 
                break; 
            }
        }
    }
    
    private void handleMouseDrag(Point point) {
        if (currentlyDraggedLabel != null) {
            
            int newBoundsX = point.x - dragStartXOffset;
            int newBoundsY = point.y - dragStartYOffset;
    
            
            
            ComponentInfo ownerComp = getComponentInfo(currentlyDraggedLabel.ownerComponentId);
            ConnectionPoint cp = ownerComp.connectionPoints.get(currentlyDraggedLabel.connectionPointName);
            FontMetrics fm = getFontMetrics(labelBusFont); 
            int ascent = fm.getAscent();
            int textHeight = fm.getHeight();
            String[] lines = currentlyDraggedLabel.originalMultiLineText.split("\n");
            int textWidth = 0;
            for (String line : lines) { textWidth = Math.max(textWidth, fm.stringWidth(line)); }
    
            int defaultX, defaultY;
             if (cp.labelXOffset != null && cp.labelYOffset != null) {
                 defaultX = currentlyDraggedLabel.anchorX + cp.labelXOffset;
                 defaultY = currentlyDraggedLabel.anchorY + cp.labelYOffset + ascent;
             } else {
                 int centerX = ownerComp.x + ownerComp.width / 2;
                 int centerY = ownerComp.y + ownerComp.height / 2;
                 boolean preferHorizontal = Math.abs(currentlyDraggedLabel.anchorX - centerX) * ownerComp.height > Math.abs(currentlyDraggedLabel.anchorY - centerY) * ownerComp.width;
                 if (preferHorizontal) {
                     defaultY = currentlyDraggedLabel.anchorY - (lines.length * textHeight / 2) + ascent;
                     if (currentlyDraggedLabel.anchorX < centerX) defaultX = currentlyDraggedLabel.anchorX - textWidth - labelOffset;
                     else defaultX = currentlyDraggedLabel.anchorX + labelOffset;
                 } else {
                     defaultX = currentlyDraggedLabel.anchorX - textWidth / 2;
                     if (currentlyDraggedLabel.anchorY < centerY) defaultY = currentlyDraggedLabel.anchorY - (lines.length * textHeight) + ascent - labelOffset;
                     else defaultY = currentlyDraggedLabel.anchorY + ascent + labelOffset;
                 }
                 if (defaultX < 2) defaultX = 2;
                 if (defaultY < textHeight) defaultY = textHeight;
             }
             
    
    
            
            
            int newLabelBaselineY = newBoundsY + ascent;
            
            int newLabelX = newBoundsX;
            
             if (cp.labelXOffset == null && cp.labelYOffset == null) {
                 int centerX = ownerComp.x + ownerComp.width / 2;
                 int centerY = ownerComp.y + ownerComp.height / 2;
                 boolean preferHorizontal = Math.abs(currentlyDraggedLabel.anchorX - centerX) * ownerComp.height > Math.abs(currentlyDraggedLabel.anchorY - centerY) * ownerComp.width;
                 if(!preferHorizontal){
                     
                     
                     newLabelX = currentlyDraggedLabel.anchorX + (newBoundsX + textWidth/2 - currentlyDraggedLabel.anchorX) ;
                 }
             }
    
    
            currentlyDraggedLabel.offsetX = newLabelX - defaultX;
            currentlyDraggedLabel.offsetY = newLabelBaselineY - defaultY;
            
    
            repaint(); 
        }
    }
    
    private void handleMouseRelease() {
        if (currentlyDraggedLabel != null) {
            currentlyDraggedLabel.isDragging = false;
            currentlyDraggedLabel = null;
            setCursor(Cursor.getDefaultCursor()); 
            repaint(); 
        }
    }

    private static class AnimationState {
        final BusID busId;
        final String value;
        final long startTime;
        final long duration; 
    
        double progress = 0.0; 
        boolean finished = false;
    
        AnimationState(BusID busId, String value, long startTime, long duration) {
            this.busId = busId;
            this.value = value;
            this.startTime = startTime;
            
            this.duration = Math.max(1, duration); 
        }
    
        
        void updateProgress(long currentTime) {
            if (finished) return;
    
            long elapsedTime = currentTime - startTime;
            this.progress = (double) elapsedTime / this.duration;
    
            if (this.progress >= 1.0) {
                this.progress = 1.0;
                this.finished = true;
            } else if (this.progress < 0.0) {
                
                this.progress = 0.0;
            }
        }
    
        boolean isFinished() {
            return finished;
        }
    
        
        
    }

    private Point getPointOnPath(BusInfo busInfo, double progress) {
        if (busInfo == null || busInfo.path == null || busInfo.path.size() < 2) {
            return null;
        }
        if (progress <= 0.0) return getAbsolutePoint(busInfo.path.get(0));
        if (progress >= 1.0) return getAbsolutePoint(busInfo.path.get(busInfo.path.size() - 1));

        
        double totalLength = 0;
        List<Point> points = new ArrayList<>();
        Point lastP = null;
        for (PathPoint pp : busInfo.path) {
            Point p = getAbsolutePoint(pp);
            if (p == null) return null; 
            if (lastP != null) {
                totalLength += lastP.distance(p);
            }
            points.add(p);
            lastP = p;
        }

        if (totalLength <= 0) { 
            return points.get(0); 
        }

        
        double targetDistance = totalLength * progress;

        
        double cumulativeDistance = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            double segmentLength = p1.distance(p2);

            if (segmentLength > 0) { 
                if (targetDistance <= cumulativeDistance + segmentLength) {
                    
                    double distanceIntoSegment = targetDistance - cumulativeDistance;
                    double ratio = distanceIntoSegment / segmentLength;

                    
                    int x = p1.x + (int) (ratio * (p2.x - p1.x));
                    int y = p1.y + (int) (ratio * (p2.y - p1.y));
                    return new Point(x, y);
                }
            }
            cumulativeDistance += segmentLength;
        }

        
        return points.get(points.size() - 1);
    }
}




class DatapathLayout {
    public CanvasSize canvasSize;
    public List<ComponentInfo> components;
    public List<BusInfo> buses;
}

class CanvasSize {
    public int width;
    public int height;
}


class ComponentInfo {
    public String id; 
    public String asset;
    public String label;
    public int x;
    public int y;
    public int width;
    public int height;
    public Map<String, ConnectionPoint> connectionPoints; 
    public Integer labelRelativeX;
    public Integer labelRelativeY;
}



class ConnectionPoint {
    public int x;
    public int y;
    public String name;
    public Integer labelXOffset; 
    public Integer labelYOffset; 
}


class BusInfo {
    public String id; 
    public List<PathPoint> path;
    public String color; 
    public int thickness;
}


class PathPoint {
    
    public String component; 
    public String point;     
    public Integer x;        
    public Integer y;        
}