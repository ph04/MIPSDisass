import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Disassembler {
    public String inputFileName;
    private byte[] inputBytes;

    private void sortBytes() {
        for (int i = 0; i < this.inputBytes.length; i += 4) {
            this.swapElements(i, i + 3);
            this.swapElements(i + 1, i + 2);
        }
    }

    public Disassembler(String inputFileName) throws IOException {
        this.inputFileName = inputFileName;

        File inputFile = new File(inputFileName);

        this.inputBytes = Files.readAllBytes(inputFile.toPath());

        this.sortBytes();
    }

    private void swapElements(int firstIndex, int secondIndex) {
        byte temp = this.inputBytes[firstIndex];
        this.inputBytes[firstIndex] = this.inputBytes[secondIndex];
        this.inputBytes[secondIndex] = temp;
    }

    public void disassemble(String outputFile) {
        // TODO: remove this crap
        for (byte a : this.inputBytes) {
            System.out.println(Integer.toHexString(a));
        }
    }
}