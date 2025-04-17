legv8-simulator/
├── src/
│   ├── simulator/
│   │   ├── core/             # Các thành phần datapath chính
│   │   │   ├── ArithmeticLogicUnit.java
│   │   │   ├── ControlUnit.java
│   │   │   ├── DataMemoryController.java
│   │   │   ├── InstructionMemory.java
│   │   │   ├── Legv8Simulator.java  # Bộ điều phối chính
│   │   │   ├── ProgramCounter.java
│   │   │   ├── RegisterFileController.java
│   │   │   └── ... (Muxes, Adders nếu cần tách riêng)
│   │   ├── instructions/     # Liên quan đến lệnh
│   │   │   ├── Instruction.java
│   │   │   ├── InstructionDefinition.java
│   │   │   ├── InstructionFactory.java
│   │   │   ├── InstructionConfigLoader.java
│   │   │   ├── RFormatInstruction.java
│   │   │   ├── IFormatInstruction.java
│   │   │   └── ... (các format khác)
│   │   ├── storage/          # Lưu trữ trạng thái
│   │   │   ├── MemoryStorage.java
│   │   │   └── RegisterStorage.java
│   │   ├── util/             # Các lớp tiện ích
│   │   │   ├── ALUOperation.java
│   │   │   ├── ALUResult.java
│   │   │   ├── ControlSignals.java
│   │   │   └── SignExtend.java
│   │   ├── exceptions/       # Các lớp Exception tùy chỉnh
│   │   │   ├── AssemblyException.java
│   │   │   ├── InvalidInstructionException.java
│   │   │   ├── InvalidPCException.java
│   │   │   └── MemoryAccessException.java
│   │   ├── assembler/        # Bộ Assembler
│   │   │   └── Assembler.java
│   │   └── gui/              # GUI sẽ nằm ở đây sau này
│   │       ├── SimulationView.java
│   │       ├── DatapathCanvas.java # Component vẽ datapath
│   │       └── ...
│   └── Main.java             # Điểm khởi chạy chính (có thể đơn giản)
├── resources/
│   └── instructions_config.csv # File cấu hình lệnh
└── ... (build files, etc.)