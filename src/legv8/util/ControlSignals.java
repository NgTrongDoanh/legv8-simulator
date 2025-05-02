package legv8.util;

public record ControlSignals(
    boolean reg2Loc, 
    boolean uncondBranch, boolean flagBranch, boolean zeroBranch, 
    boolean memRead, boolean memToReg, boolean memWrite, 
    boolean flagWrite, 
    boolean aluSrc, 
    int aluOp,
    boolean regWrite,
    int operation
) {
    public static final ControlSignals NOP = new ControlSignals(
        false, false, false, false, 
        false, false, false, 
        false, 
        false, 0,
        false,
        0
    );

    public static final ControlSignals HALT = new ControlSignals(
        true, true, true, true,
        true, true, true,
        true,
        true, 0,
        true,
        0
    );

    @Override
    public String toString() {
        return String.format(
            "ControlSignals [reg2Loc=%b, uncondBranch=%b, flagBranch=%b, zeroBranch=%b, memRead=%b, memToReg=%b, memWrite=%b, flagWrite=%b, aluSrc=%b, aluOp=%d, regWrite=%b, operation=%d]",
            reg2Loc, uncondBranch, flagBranch, zeroBranch,
            memRead, memToReg, memWrite,
            flagWrite,
            aluSrc, aluOp,
            regWrite, 
            operation
        );
    }
}
