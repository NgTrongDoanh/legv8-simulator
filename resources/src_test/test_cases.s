// LEGv8 Test Cases
// This file contains test cases for 8 common LEGv8 instructions

// Test Case 1: ADD instruction
// Test adding two registers
MOVZ    X1, #5, LSL #0    // X1 = 5
MOVZ    X2, #3, LSL #0    // X2 = 3
ADD     X3, X1, X2        // X3 = X1 + X2 (should be 8)

// Test Case 2: SUB instruction
// Test subtracting two registers
MOVZ    X4, #10, LSL #0   // X4 = 10
MOVZ    X5, #4, LSL #0    // X5 = 4
SUB     X6, X4, X5        // X6 = X4 - X5 (should be 6)

// Test Case 3: LDUR instruction
// Test loading from memory
MOVZ    X7, #100, LSL #0  // X7 = 100 (memory address)
MOVZ    X8, #42, LSL #0   // X8 = 42
STUR    X8, [X7, #0]      // Store 42 at address 100
LDUR    X9, [X7, #0]      // Load from address 100 into X9 (should be 42)

// Test Case 4: STUR instruction
// Test storing to memory
MOVZ    X10, #200, LSL #0 // X10 = 200 (memory address)
MOVZ    X11, #99, LSL #0  // X11 = 99
STUR    X11, [X10, #0]    // Store 99 at address 200
LDUR    X12, [X10, #0]    // Load from address 200 into X12 (should be 99)

// Test Case 5: B instruction
// Test unconditional branch
B       branch_target     // Branch to branch_target
MOVZ    X13, #1, LSL #0   // This should be skipped
branch_target:
MOVZ    X14, #2, LSL #0   // This should be executed

// Test Case 6: CBZ instruction
// Test conditional branch if zero
MOVZ    X15, #0, LSL #0   // X15 = 0
CBZ     X15, zero_branch  // Branch if X15 is zero
MOVZ    X16, #1, LSL #0   // This should be skipped
zero_branch:
MOVZ    X17, #2, LSL #0   // This should be executed

// Test Case 7: ADDI instruction
// Test adding immediate value
MOVZ    X18, #10, LSL #0  // X18 = 10
ADDI    X19, X18, #5      // X19 = X18 + 5 (should be 15)

// Test Case 8: AND instruction
// Test logical AND operation
MOVZ    X20, #0xFF, LSL #0 // X20 = 0xFF
MOVZ    X21, #0x0F, LSL #0 // X21 = 0x0F
AND     X22, X20, X21      // X22 = X20 & X21 (should be 0x0F)

// End of test cases 