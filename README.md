# MIPSDisass

A very bad and stupid Java MIPS disassembler.

**Note**: this disassembler was entirely tested with the MARS assembler functionality,
so if used on other binaries, compiled by other compilers (such as *gcc*, which has the
MIPS target option), the disassembled instructions are not guaranteed to be exact.

## TODO list

- [x] fix shifting instructions
- [x] implement mul and div instructions
- [x] implement branch instructions
- [ ] negative numbers are incorrectly displayed
- [ ] print instructions on output file with a new method
- [ ] build the jar file
- [ ] pass the input file and the output file from argv
