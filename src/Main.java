import java.io.IOException;

public class Main {
    public static void main(String[] argv) {
        try {
            String inputFile = argv[0];
            String outputFile = argv[1];

            Disassembler disassembler;

            try {
                disassembler = new Disassembler(inputFile);

                disassembler.disassemble();
                disassembler.printOutputLines();

                try {
                    disassembler.outputToFile(outputFile);
                } catch (IOException e) {
                    e.printStackTrace();

                    System.out.println("There was an error trying to initialize the FileWriter for the output file.");
                }
            } catch (IOException e) {
                e.printStackTrace();

                System.out.println("The input file does not exist.");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();

            System.out.println("The arguments are not correctly specified.");
        }
    }
}