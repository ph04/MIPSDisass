import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Disassembler {
    public String inputFileName;
    private JSONArray opcodes;
    private byte[] inputBytes;
    private int inputBitsLength;
    private String[] inputBits;
    private String[] outputLines;
    private String[] registers;

    private String byteToBitString(int index) {
        return String.format("%8s", Integer.toBinaryString(Byte.toUnsignedInt(this.inputBytes[index]))).replace(' ', '0');
    }

    private void generateBitArray() {
        // store every 32 bit line as a String
        // inverting the order of the bytes
        // (since assembly MIPS is big endian)
        for (int i = 0, j = 0; i < this.inputBytes.length; i += 4, j++) {
            this.inputBits[j] = this.byteToBitString(i + 3) + this.byteToBitString(i + 2) + this.byteToBitString(i + 1) + this.byteToBitString(i);
        }
    }

    private void parseJSONFile() throws ParseException, IOException {
        // initialize the JSON parser
        JSONParser jsonParser = new JSONParser();

        // create the file handler
        FileReader opcodeFile = new FileReader("Opcodes.json");

        // read the JSON file
        Object obj = jsonParser.parse(opcodeFile);

        // create the JSONArray from the parsed JSON object
        this.opcodes = (JSONArray) obj;
    }

    public Disassembler(String inputFileName) throws IOException {
        this.inputFileName = inputFileName;

        // create the file handler
        File inputFile = new File(inputFileName);

        try {
            // parse the JSON file containing the opcodes
            this.parseJSONFile();

            // read the bytes of the binary
            this.inputBytes = Files.readAllBytes(inputFile.toPath());

            this.inputBitsLength = this.inputBytes.length / 4;

            // initialize bit array
            this.inputBits = new String[this.inputBitsLength];

            this.generateBitArray();

            this.outputLines = new String[this.inputBitsLength];

            // registers in order
            this.registers = new String[]{
                "$zero", "$at", "$v0", "$v1", "$a0", "$a1", "$a2", "$a3",
                "$t0",   "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7",
                "$s0",   "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
                "$t9",   "$t9", "$k0", "$k1", "$gp", "$sp", "$fp", "$ra"
            };
        } catch (IOException e) {
            e.printStackTrace();

            System.out.println("The Opcodes.json file is not in the root of the folder.");
        } catch (ParseException e) {
            e.printStackTrace();

            System.out.println("There was an error trying to read the JSON file, thus the Opcode.json file is corrupted.");
        }
    }

    public void printOutputLines() {
        for (String line : this.outputLines) {
            System.out.print(line);
        }
    }

    private int bitsToInt(int lineIndex, int index1, int index2) {
        return Integer.parseInt(this.inputBits[lineIndex].substring(index1, index2), 2);
    }

    public void disassemble(String outputFile) {
        for (int i = 0; i < this.inputBitsLength; i++) {
            String line = this.inputBits[i];

            // special check for nop instruction
            // because fails due to other similar instructions
            if (line.equals("00000000000000000000000000000000")) {
                this.outputLines[i] = "nop\n";

                continue;
            }

            // check the first 6 bits of the line
            String mnemonicBits = line.substring(0, 6);

            // if the first 6 bits are "000000"
            // it means that the instruction is
            // of type "R" (except for few cases)
            if (mnemonicBits.equals("000000")) {
                // check the last 6 bist of the line
                String funcBits = line.substring(26, 32);

                for (Object obj : this.opcodes) {
                    // get instruction
                    JSONObject instructionObject = (JSONObject) ((JSONObject) obj).get("instruction");

                    // get bits
                    String objectBits = (String) instructionObject.get("bits");

                    // get func
                    String objectFunc = (String) instructionObject.get("func");

                    // get type
                    String objectType = (String) instructionObject.get("type");

                    if (mnemonicBits.equals(objectBits) && objectFunc.equals(funcBits) && objectType.equals("R")) {
                        // get name
                        String objectName = (String) instructionObject.get("name");

                        // get shamt
                        String objectShamt = (String) instructionObject.get("shamt");

                        // it the "shamt" field is null
                        // the instruction is a shifter
                        if (objectShamt == null) {
                            // get rt
                            String rtValue = this.registers[this.bitsToInt(i, 11, 16)];

                            // get rd
                            String rdValue = this.registers[this.bitsToInt(i, 16, 21)];

                            // get imm
                            String saValue = Integer.toString(this.bitsToInt(i, 21, 26));

                            // note: they are inverted in the binary!
                            this.outputLines[i] = objectName + " " + rdValue + ", " + rtValue + ", " + saValue;

                            break;
                        }

                        // get rs
                        String rsValue = this.registers[this.bitsToInt(i, 6, 11)];

                        // get rt
                        String rtValue = this.registers[this.bitsToInt(i, 11, 16)];

                        // get rd
                        String rdValue = this.registers[this.bitsToInt(i, 16, 21)];

                        // note: they are inverted in the binary!
                        this.outputLines[i] = objectName + " " + rdValue + ", " + rsValue + ", " + rtValue;

                        break;
                    } else {
                        // get name
                        String objectName = (String) instructionObject.get("name");

                        if (objectType.equals("S") && objectFunc.equals(funcBits)) {
                            this.outputLines[i] = objectName;

                            break;
                        }
                    }
                }
            } else {
                for (Object obj : this.opcodes) {
                    // get instruction
                    JSONObject instructionObject = (JSONObject) ((JSONObject) obj).get("instruction");

                    // get bits field
                    String objectBits = (String) instructionObject.get("bits");

                    if (mnemonicBits.equals(objectBits)) {
                        // get name
                        String objectName = (String) instructionObject.get("name");

                        // get rs
                        String rsValue = this.registers[this.bitsToInt(i, 6, 11)];

                        // get rt
                        String rtValue = this.registers[this.bitsToInt(i, 11, 16)];

                        // get imm
                        String immValue = Integer.toString(this.bitsToInt(i, 16, 32));

                        if (mnemonicBits.charAt(0) == '1') {
                            // note: they are inverted in the binary!
                            this.outputLines[i] = objectName + " " + rtValue + ", " + immValue + "(" + rsValue + ")";
                        } else if (objectName.equals("lui")) { // this instruction is an exception
                            // note: they are inverted in the binary!
                            this.outputLines[i] = objectName + " " + rtValue + ", " + immValue;
                        } else {
                            // note: they are inverted in the binary!
                            this.outputLines[i] = objectName + " " + rtValue + ", " + rsValue + ", " + immValue;
                        }

                        break;
                    }
                }
            }

            this.outputLines[i] += "\n"; // TODO: not sure if this is needed anyway
        }
    }
}