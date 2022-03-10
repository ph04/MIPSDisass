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

            String mnemonicBits = line.substring(0, 6);

            // if the first 6 bits are "000000"
            // it means that the instruction is
            // of type "R" (except for few cases)
            if (mnemonicBits.equals("000000") || mnemonicBits.equals("010000")) {
                String funcBits = line.substring(26, 32);

                for (Object obj : this.opcodes) {
                    JSONObject instructionObject = (JSONObject) ((JSONObject) obj).get("instruction");

                    String objectBits = (String) instructionObject.get("bits");
                    String objectFunc = (String) instructionObject.get("func");
                    String objectType = (String) instructionObject.get("type");

                    if (mnemonicBits.equals(objectBits) && objectFunc.equals(funcBits)) {
                        String objectName = (String) instructionObject.get("name");
                        String objectShamt = (String) instructionObject.get("shamt");

                        if (objectType.contains("R")) {

                            // it the "shamt" field is null
                            // the instruction is a shifter
                            if (objectShamt == null) {
                                String rtValue = this.registers[this.bitsToInt(i, 11, 16)];
                                String rdValue = this.registers[this.bitsToInt(i, 16, 21)];
                                String saValue = Integer.toString(this.bitsToInt(i, 21, 26));

                                this.outputLines[i] = objectName + " " + rdValue + ", " + rtValue + ", " + saValue;

                                break;
                            }

                            String rsValue = this.registers[this.bitsToInt(i, 6, 11)];
                            String rtValue = this.registers[this.bitsToInt(i, 11, 16)];

                            // the mul and div instructions
                            // have unusual formats than the others
                            if (objectType.contains("d")) {
                                if (rtValue.equals("$zero")) {
                                    if (rsValue.equals("$zero")) {
                                        String rdValue = this.registers[this.bitsToInt(i, 16, 21)];

                                        this.outputLines[i] = objectName + " " + rdValue;
                                    } else {
                                        this.outputLines[i] = objectName + " " + rsValue;
                                    }
                                } else {
                                    this.outputLines[i] = objectName + " " + rsValue + ", " + rtValue;
                                }
                            } else {
                                String rdValue = this.registers[this.bitsToInt(i, 16, 21)];

                                if (rsValue.equals("$zero")) {
                                    this.outputLines[i] = objectName + " " + rtValue + ", " + rdValue;
                                } else if (rsValue.equals("$a0")){
                                    // this instruction had to be hardcoded
                                    // since there are not enough information
                                    // stored in the Opcode.json file to
                                    // distinguish the two opcodes
                                    this.outputLines[i] = "mtc0" + " " + rtValue + ", " + rdValue;
                                } else if (objectType.contains("j")) {
                                    if (rdValue.equals("$zero")) {
                                        this.outputLines[i] = objectName + " " + rsValue;
                                    } else {
                                        this.outputLines[i] = objectName + " " + rdValue + ", " + rsValue;
                                    }
                                } else {
                                    this.outputLines[i] = objectName + " " + rdValue + ", " + rsValue + ", " + rtValue;
                                }
                            }
                        } else if (objectType.contains("S")) {
                            // note: 'break' instruction is untested
                            // but should work without any major issue
                            // if any error are noticed please report
                            // them on the github page, but don't
                            // expect me to fix them, this code is
                            // complete trash, and i think you can
                            // notice that by yourself, it started
                            // as a meme and then i coded it
                            // just for fun, so don't expect anything
                            // from me and from this trash code
                            this.outputLines[i] = objectName;
                        }

                        break;
                    }
                }
            } else {
                for (Object obj : this.opcodes) {
                    JSONObject instructionObject = (JSONObject) ((JSONObject) obj).get("instruction");

                    String objectBits = (String) instructionObject.get("bits");
                    String objectType = (String) instructionObject.get("type");

                    if (objectType.equals("B")) {
                        String objectName = (String) instructionObject.get("name");
                        String rsValue = this.registers[this.bitsToInt(i, 6, 11)];
                        String offsetValue = Integer.toString(this.bitsToInt(i, 16, 32));

                        this.outputLines[i] = objectName + " " + rsValue + ", " + offsetValue;

                        break;
                    }

                    if (mnemonicBits.equals(objectBits)) {
                        String objectName = (String) instructionObject.get("name");
                        String rsValue = this.registers[this.bitsToInt(i, 6, 11)];
                        String rtValue = this.registers[this.bitsToInt(i, 11, 16)];
                        String immValue = Integer.toString(this.bitsToInt(i, 16, 32));

                        // the instructions that start with a '1'
                        // are memory access instructions, and must
                        // be printed in a different format
                        if (mnemonicBits.charAt(0) == '1') {
                            this.outputLines[i] = objectName + " " + rtValue + ", " + immValue + "(" + rsValue + ")";
                        } else if (objectType.contains("J")) {
                            String targetValue = Integer.toHexString(this.bitsToInt(i, 6, 32));

                            this.outputLines[i] = objectName + " 0x" + targetValue;
                        } else if (objectName.equals("lui")) { // this instruction is an exception
                            this.outputLines[i] = objectName + " " + rtValue + ", " + immValue;
                        } else {
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