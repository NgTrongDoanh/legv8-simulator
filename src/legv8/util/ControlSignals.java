/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.util;

/**
 * ControlSignals is a record that represents the control signals used in the LEGv8 architecture.
 * It contains various boolean flags and integer values that control the behavior of the processor.
 */
public record ControlSignals(
    char reg2Loc, 
    char uncondBranch, char flagBranch, char zeroBranch, 
    char memRead, char memToReg, char memWrite, 
    char flagWrite, 
    char aluSrc, 
    int aluOp,
    char regWrite,
    int operation
) {
    public static final ControlSignals NOP = new ControlSignals(
        '0', '0', '0', '0', 
        '0', '0', '0', 
        '0', 
        '0', 0,
        '0',
        0
    );

    public static final ControlSignals HALT = new ControlSignals(
        '1', '1', '1', '1',
        '1', '1', '1',
        '1',
        '1', 0,
        '1',
        0
    );

    @Override
    public String toString() {
        return String.format(
            "ControlSignals [reg2Loc=%c, uncondBranch=%c, flagBranch=%c, zeroBranch=%c, memRead=%c, memToReg=%c, memWrite=%c, flagWrite=%c, aluSrc=%c, aluOp=%d, regWrite=%c, operation=%d]",
            reg2Loc, uncondBranch, flagBranch, zeroBranch,
            memRead, memToReg, memWrite,
            flagWrite,
            aluSrc, aluOp,
            regWrite, 
            operation
        );
    }
}
