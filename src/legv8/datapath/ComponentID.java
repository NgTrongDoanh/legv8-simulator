package legv8.datapath;


public enum ComponentID {
    PROGRAM_COUNTER,
    INSTRUCTION_MEMORY, 
    SPLITTER, 
    MUX_REGFILESrc, 
    REGISTERS_FILE,      
    EXTRACTOR,
    CONTROL_UNIT,       
    MUX_ALUSrc, 
    ALU,        
    ALU_CONTROL,   
    DATA_MEMORY,      
    MUX_WB_REGFILE,  
    PC_ADDER4,      
    SHIFT_LEFT_2, 
    BR_ADDER, 
    MUX_PCSrc,  
    N_FLAG,    
    Z_FLAG,    
    C_FLAG,    
    V_FLAG,    
    BR_FLAG_AND, 
    BR_ZERO_AND, 
    BR_OR,
    CONSTANT_4
}