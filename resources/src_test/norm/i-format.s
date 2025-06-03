// Test I-format: ADDI, SUBI, ANDI, ORRI
    ADDI X13, X1, #4       // X13 = 0x14
    SUBI X14, X1, #4       // X14 = 0x0C
    ANDI X15, X1, #0xF     // X15 = 0x0
    ORRI X16, X1, #0x3     // X16 = 0x13