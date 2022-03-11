# MIPSDisass

A very bad and stupid Java MIPS disassembler.

**Note**: this disassembler was (almost) entirely tested with the MARS assembler functionality,
so if used on binaries assembled by other assemblers (such as *gcc*, which has the
MIPS target option), the disassembled instructions are not guaranteed to be correct.

## TODO list

- [x] fix shifting instructions
- [x] implement mul and div instructions
- [x] implement branch instructions
- [x] print instructions on output file with a new method
- [x] pass the input file and the output file from argv
- [x] negative numbers are incorrectly displayed
- [ ] fix branch instructions because they are broken for some reason
- [ ] build the jar file
