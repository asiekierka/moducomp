; Code by GreaseMonkey

	.org $FE000
code_start:
	; Clear interrupts
	cli

	; Check for amount of RAM installed (in slot 0 for now)
detectRAM:
	; Begin
	move.w @9, #$007F
	move.b @13, #$42
detectRAMLoop:
	st.b $00000, @9, @13
	ld.b @14, $00000, @9
	cmp.b @13, @14
	jnz detectRAMEnd
	add.w @9, #$0080
detectRAMEnd:
	lsr.w @9, #7
	; Check for terminal
detectTerminal:
	; Start looking
	move.w @8, #$0003
detectionLoop:
	ld.b @14, $FD000, @8
	cmp.w @14, #$0001
	jz detectionEnd
	add.w @8, #$0100
	cmp.w @8, #$1003
	jz halt
	jmp detectionLoop
detectionEnd:
	sub.b @8, #3
	; Check if any RAM is installed
	cmp.w @9, #$0000
	jz halt

	; Set stack pointer
	move.w @15, #$0080

	; Set up our interrupt vector.
	move.w @1, #int_vec
	st.w $FFF80, @1
	move.b @1, #$0F
	st.b $FFF82, @1

	jsr printMemory
	sei
prompt:
	move.w @7, #$0000 ; Char begin here

	; Allow echoing
	move.w @1, #$03
	st.w $FD008, @8, @1

	move.w @1, #prompt_string
	jsr putsF

	; Le halt
halt:	hlt
	jmp halt

; @1 - value, @2 - bytes
; @2 and @12-@14 will get clobbered
printHex:
	move.w @14, @1
	move.w @13, @0
printHexLoop:
	cmp.b @2, @0
	jz printHexEnd
	move.w @13, @14
	and.w @13, #$F000
	lsr.w @13, #12
	ld.b @12, hexChars, @13
	st.b $FD00A, @8, @12
	st.b $FD00B, @8, @0
	lsl.w @14, #4
	sub.b @2, #1
	jmp printHexLoop
printHexEnd:
	ret

skipSpaces:
	add.w @14, #1
	ld.b @13, $00000, @14
	cmp.b @13, #32
	jz skipSpaces
	ret

getHex:
	move.w @12, @0
getHexLoop:
	move.w @13, @0
	ld.b @13, $00000, @14
	add.w @14, #1
	cmp.b @13, #48
	jc getHexEnd
	cmp.b @13, #58
	jnc getHexUp
	sub.b @13, #48
getHexSet:
	lsl.w @12, #4
	or.w @12, @13
	jmp getHexLoop
getHexUp:
	cmp.b @13, #71
	jc getHexUp2
	sub.b @13, #32
	cmp.b @13, #71
	jnc getHexEnd
getHexUp2:
	cmp.b @13, #65
	jc getHexEnd
	sub.b @13, #55
	jmp getHexSet
getHexEnd:
	sub.w @14, #1
	ret

hexChars: .db "0123456789ABCDEF"
prompt_string: .db "$ ", 0

printMemory:
	; Print out memory available
	move.w @1, @9
	asl.w @1, #3
	move.w @2, #$0004
	jsr printHex
	move.w @1, #ram_amount_string
	jsr putsF
	jsr newlineF
	ret

char_print:
	move.w @5, @0
	move.w @6, #$10 ; Default length
	jsr skipSpaces
	jsr getHex
	move.w @5, @12 ; Set @5 to pos
char_print_fin:
	cmp.w @6, #0
	jz char_print_fin_fin
	move.w @1, @5
	move.w @2, #4
	jsr printHex
	move.w @1, #print_sep_string
	jsr putsF
	move.w @11, #4
	jmp char_print_loop
char_print_space:
	move.w @1, #32
		st.b $FD00A, @8, @1
		st.b $FD00B, @8, @0
char_print_loop:
	cmp.w @6, #0
	jz char_print_fin_nl
	ld.b @1, $00000, @5
	asl.w @1, #8
	move.w @2, #2
	jsr printHex
	add.w @5, #1
	sub.w @6, #1
	sub.w @11, #1
	cmp.w @11, #0
	jnz char_print_space
	jsr newlineF
	jmp char_print_fin
char_print_fin_nl:
	jsr newlineF
char_print_fin_fin:
	jmp parse_end

char_ram:
	jsr printMemory
	jmp parse_end

char_enter: ; Parse!
	add.w @15, #9 ; We won't be coming back.
	move.w @14, #0 ; Pointer
	ld.b @1, $00000
	cmp.b @1, #80
	jz char_print
	cmp.b @1, #82
	jz char_ram
parse_end:
	move.w @14, @0
parse_end_loop:
	st.b $00000, @14, @0
	add.w @14, #1
	cmp.w @14, @7
	jc parse_end_loop
	jmp prompt

char_backspace: ; Remove char
	sub.w @7, #1
	jmp int_vec_end

int_vec_char:
	; Load char
	ld.w @1, $FD010, @8
	; Check!
	cmp.w @1, #13 ; Enter
	jz char_enter
	cmp.w @1, #127 ; Backspace
	jz char_backspace
	cmp.w @1, #9 ; Tab
	jnz char_range
	move.w @1, #32
char_range:
	; Valid range?
	cmp.w @1, #32
	jc int_vec_end ; Too small
	cmp.w @1, #127
	jnc int_vec_end ; Too large or equal
char_add: ; Whee
	st.b $00000, @7, @1
	add.w @7, #1
	jmp int_vec_end

int_vec:
	; Stash some registers on the stack.
	sub.w @15, #2
	st.w $00000, @1
	sub.w @15, #2
	st.w $00000, @2

	; Check for interrupt number 0
	ld.b @1, $FFF84
	and.b @1, #$01
	cmp.b @1, #$01
	jz int_vec_char

int_vec_end:
	; Clear all interrupts because we are lazy.
	move.w @1, #$FFFF
	st.w $FFF84, @1
	st.w $FFF86, @1

	; Restore registers.
	ld.w @2, $00000
	add.w @15, #2
	ld.w @1, $00000
	add.w @15, #2

	; Return.
	popf
	ret

newlineF:
	st.b $FD00C, @8, @0
	ret

	; INPUT:
	;   @1 = input string in $Fxxxx bank
	; CLOBBERS: @1, @13
putsF:
	move.w @13, @1
putsF_lp1:
	ld.b @1, $F0000, @13
	and.b @1, @1
	jz putsF_ret
		add.w @13, #1
		st.b $FD00A, @8, @1
		st.b $FD00B, @8, @0
		jmp putsF_lp1
putsF_ret:
	ret

no_ram_string:
	.db "NO RAM FOUND", 0
ram_amount_string:
	.db "0 BYTES OK", 0
print_sep_string:
	.db " | ", 0

vec_cli_idle:
	.dw halt
	.db $0F

	.org $FFF80
code_end:
	.dw $0000 ; version identifier
	.db "Areia-1 BIOS ROM", $00

	.org $FFFFC
	.dd code_start

