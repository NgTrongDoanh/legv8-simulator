// Test memory access (LDUR, STUR)
    MOVZ X20, #0x5000      // base address
    MOVZ X21, #0x1122      // data to store
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
