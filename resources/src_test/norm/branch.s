// Test branch: B, BL, BR
    MOVZ X30, #0           // set up link register
    B label1               // jump over trap
    MOVZ X0, #0xDEAD       // trap (should be skipped)
label1:
    MOVZ X29, #0xBEEF

// Test CBZ/CBNZ
    MOVZ X5, #0
    CBZ X5, cb_target1     // should jump
    MOVZ X0, #0x9999       // trap
cb_target1:
    MOVZ X6, #1
    CBNZ X6, cb_target2    // should jump
    MOVZ X0, #0x8888       // trap
cb_target2:
    ADDI X31, X31, #3

// Test conditional branches (requires proper flag setting before)
    ADDS X1, X1, XZR       // set Z=0
    B.NE cond_target       // should jump
    MOVZ X0, #0x1234       // trap
cond_target:
    ADDI X31, X31, #3
    
