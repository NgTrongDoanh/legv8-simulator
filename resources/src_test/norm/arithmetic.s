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