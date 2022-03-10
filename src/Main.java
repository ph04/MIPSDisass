import java.io.IOException;

public class Main {
    public static void main(String[] argv) {
        Disassembler disassembler;

        try {
            disassembler = new Disassembler("../../asmips/nop_test3");

            disassembler.disassemble("nop_test.s");

            disassembler.printOutputLines();
        } catch (IOException e) {
            e.printStackTrace();

            System.out.println("The input file does not exist.");
        }
    }
}