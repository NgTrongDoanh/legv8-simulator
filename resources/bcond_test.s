// bcond_test.s
ADDI X1, XZR, #10
ADDI X2, XZR, #5
SUBS XZR, X1, X2     // Set flags: X1 > X2 -> N=0, Z=0, C=1, V=0 (GE, GT, HI, CS ...)
B.EQ skip_eq         // Should not branch
ADDI X3, X3, #1      // <<== SỬA THÀNH ADDI
skip_eq:
B.GE branch_ge       // Should branch (N==V is true)
ADDI X4, X4, #1      // <<== SỬA THÀNH ADDI (Skipped)
branch_ge:
ADDI X5, X5, #1      // <<== SỬA THÀNH ADDI

SUBS XZR, X2, X1     // Set flags: X2 < X1 -> N=1, Z=0, C=0, V=0 (LT, MI, CC ...)
B.LT branch_lt       // Should branch (N!=V is true)
ADDI X6, X6, #1      // <<== SỬA THÀNH ADDI (Skipped)
branch_lt:
ADDI X7, X7, #1      // <<== SỬA THÀNH ADDI

ADDI X10, XZR, #0
SUBS XZR, X10, X10   // Set flags: Z=1
B.NE skip_ne         // Should not branch
ADDI X8, X8, #1      // <<== SỬA THÀNH ADDI
skip_ne:

B #0                 // Infinite loop at the end