package legv8.util;

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