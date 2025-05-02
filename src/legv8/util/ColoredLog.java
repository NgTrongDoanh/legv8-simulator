package legv8.util;

public class ColoredLog {
    // Mã ANSI cho màu
    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String BROWN = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String MAGENTA = "\u001B[35m";


    public static final String SUCCESS = GREEN + "[+] " + RESET;
    public static final String FAILURE = BROWN + "[-] " + RESET;
    public static final String INFO = BLUE + "[*] " + RESET;
    public static final String WARNING = YELLOW + "[!] " + RESET;
    public static final String ERROR = RED + "[X] " + RESET;
    public static final String PENDING = CYAN + "[~] " + RESET;
    public static final String START_PROCESS = MAGENTA + "[>] " + RESET;
    public static final String END_PROCESS = MAGENTA + "[<] " + RESET;

    public static void main(String[] args) {
        System.out.println(GREEN + "[+] Success!" + RESET);
        System.out.println(RED + "[-] Failure!" + RESET);
        System.out.println(YELLOW + "[!] Warning!" + RESET);
        System.out.println(BLUE + "[*] Info" + RESET);
    }
}
