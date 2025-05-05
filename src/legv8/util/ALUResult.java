/**
 * @author TrDoanh, Giahuy
 * @version 1.0 --- There may be bugs :) Be careful! 
 */

package legv8.util;

/**
 * ALUResult is a record that represents the result of an Arithmetic Logic Unit (ALU) operation in the LEGv8 architecture.
 * It contains the result of the operation and various flags indicating the status of the operation.
 */
public record ALUResult(
    long result,
    boolean negativeFlag, 
    boolean zeroFlag,     
    boolean carryFlag,    
    boolean overflowFlag  
) {
    @Override
    public String toString() {
        return String.format("Res=0x%016X (N=%b Z=%b C=%b V=%b)",
                             result, negativeFlag, zeroFlag, carryFlag, overflowFlag);
    }
}