// Arithmetic Instructions (R-type)
ADD     X8, X0, X1              // X8 = X0 + X1 (0x1234 + 0x5678)
ADDS    X9, X0, X1              // X9 = X0 + X1, set flags
SUB     X10, X1, X0             // X10 = X1 - X0 (0x5678 - 0x1234)
SUBS    X11, X1, X0             // X11 = X1 - X0, set flags

// Logical Instructions (R-type)
AND     X12, X0, X2             // X12 = X0 & X2 (0x1234 & 0xFFFF)
ANDS    X13, X0, X2             // X13 = X0 & X2, set flags
ORR     X14, X0, X1             // X14 = X0 | X1 (0x1234 | 0x5678)
EOR     X15, X0, X1             // X15 = X0 ^ X1 (0x1234 ^ 0x5678)

// Shift Instructions (R-type)
LSL     X16, X0, X5             // X16 = X0 << 8 (0x1234 << 8)
LSR     X17, X0, X5             // X17 = X0 >> 8 (0x1234 >> 8)
ASR     X18, X6, X5             // X18 = X6 >> 8 (arithmetic, sign-extended)

// Multiplication and Division Instructions (R-type)
MUL     X19, X0, X3             // X19 = X0 * X3 (0x1234 * 2)
SMULH   X20, X0, X3             // X20 = high bits of signed X0 * X3
UMULH   X21, X0, X3             // X21 = high bits of unsigned X0 * X3
SDIV    X22, X0, X7             // X22 = X0 / X7 (0x1234 / 4)
UDIV    X23, X0, X7             // X23 = X0 / X7 (unsigned)

// Immediate Arithmetic Instructions (I-type)
ADDI    X24, X0, #0x100         // X24 = X0 + 0x100
ADDIS   X25, X0, #0x100         // X25 = X0 + 0x100, set flags
SUBI    X26, X1, #0x100         // X26 = X1 - 0x100
SUBIS   X27, X1, #0x100         // X27 = X1 - 0x100, set flags

// Immediate Logical Instructions (I-type)
ANDI    X28, X0, #0xFF          // X28 = X0 & 0xFF
ANDIS   X29, X0, #0xFF          // X29 = X0 & 0xFF, set flags
ORI     X30, X0, #0xFF          // X30 = X0 | 0xFF
EORI    X31, X0, #0xFF          // X31 = X0 ^ 0xFF

// Load/Store Instructions (D-type)
LDUR    X8, [X4, #0]            // Load 64-bit from array[0]
STUR    X0, [X4, #8]            // Store X0 to array[2]
LDURB   X9, [X4, #16]           // Load byte from byte
STURB   X2, [X4, #16]           // Store X2[7:0] to byte
LDURH   X10, [X4, #18]          // Load halfword from half
STURH   X2, [X4, #18]           // Store X2[15:0] to half
LDURSW  X11, [X4, #20]          // Load sign-extended word from word
STURW   X2, [X4, #20]           // Store X2[31:0] to word

// Branch Instructions (B-type and C-type)
B       loop                    // Unconditional branch to loop
