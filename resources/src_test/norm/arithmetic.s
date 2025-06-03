// Test program for ADD, ADDS, SUB, SUBS, AND, ORR, EOR
    MOVZ X1, #0x10         // X1 = 0x10
    MOVZ X2, #0x05         // X2 = 0x05
    ADD  X3, X1, X2        // X3 = X1 + X2 = 0x15
    ADDS X4, X1, X2        // X4 = X1 + X2 (sets flags)
    SUB  X5, X1, X2        // X5 = 0x0B
    SUBS X6, X1, X2        // X6 = 0x0B (sets flags)
    AND  X7, X1, X2        // X7 = 0x00
    ORR  X8, X1, X2        // X8 = 0x15
    EOR  X9, X1, X2        // X9 = 0x15

// Test shift
    MOVZ X10, #0x1         // X10 = 1
    LSL X11, X10, #3       // X11 = 8
    LSR X12, X11, #2       // X12 = 2

// Test I-format: ADDI, SUBI, ANDI, ORRI
    ADDI X13, X1, #4       // X13 = 0x14
    SUBI X14, X1, #4       // X14 = 0x0C
    ANDI X15, X1, #0xF     // X15 = 0x0
    ORRI X16, X1, #0x3     // X16 = 0x13

// Test memory access (LDUR, STUR)
    MOVZ X20, #0x50, LSL #16      // base address
    MOVZ X21, #0xdead, LSL #16      // data to store
    MOVK X21, #0xbeef
    STUR X21, [X20, #0]    // Mem[0x5000] = X21 (64-bit)
    LDUR X22, [X20, #0]    // X22 = Mem[0x5000] (should be 0x1122)

// Test byte and halfword store/load
    MOVZ X23, #0xAB        // data
    STURB X23, [X20, #8]   // store byte to offset +8
    LDURB X24, [X20, #8]   // X24 = 0xAB (zero extended)

    MOVZ X25, #0x1234
    STURH X25, [X20, #16]  // store halfword
    LDURH X26, [X20, #16]  // X26 = 0x1234 (zero extended)

// Test word access
    MOVZ X27, #0xAAAA
    STURW X27, [X20, #24]  // store word
    LDURSW X28, [X20, #24] // X28 = 0xAAAA (sign extended)

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
    MOVZ XZR, #0xdead

// Test conditional branches (requires proper flag setting before)
    ADDS X1, X1, XZR       // set Z=0
    B.NE cond_target       // should jump
    MOVZ X0, #0x1234       // trap
cond_target:
    MOVZ XZR, #0xbeef
