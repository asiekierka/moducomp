; Code by asiekierka

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
	cmp.w @9, #$007F
	jz detectRAM64K
	jmp detectRAMLoop
detectRAM64K:
	move.w @9, #$0200
	jmp detectTerminal
detectRAMEnd:
	lsr.w @9, #7
	; Check for terminal
detectTerminal:
	; Start looking
	move.w @8, @0
detectionLoop:
	ld.b @14, $FD003, @8
	cmp.w @14, #$0001
	jz detectionEnd
	add.w @8, #$0100
	cmp.w @8, #$1000
	jz halt
	jmp detectionLoop
detectionEnd:
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

	; Set color
	move.w @1, #$000F
	st.b $FD012, @8, @1
	st.w $FD00A, @8, @1
	move.w @1, #$7F04
	st.w $FD010, @8, @1

	jsr printMemory
prompt:
	st.b $FD00E, @8, @0
	move.w @7, #$0002 ; Char begin here

	; Allow echoing
	move.w @1, #$07
	st.w $FD004, @8, @1

	move.w @1, #prompt_string
	jsr putsF

	; Le halt
	sei
halt:	hlt
	jmp halt

; @1 - value, @2 - bytes
; @2 and @12-@14 will get clobbered
printHex:
	move.w @14, @1
printHexLoop:
	cmp.b @2, @0
	jz printHexEnd
	move.w @13, @14
	and.w @13, #$F000
	lsr.w @13, #12
	ld.b @12, hexChars, @13
	st.b $FD008, @8, @12
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

isHex:
	cmp.b @13, #48
	jc isHexNot
	cmp.b @13, #58
	jnc isHexUp
	sub.b @13, #48
	ret
isHexUp:
	cmp.b @13, #71
	jc isHexUp2
	sub.b @13, #32
	cmp.b @13, #71
	jnc isHexNot
isHexUp2:
	cmp.b @13, #65
	jnc isHexUp3
isHexNot:
	move.b @13, #255
	ret
isHexUp3:
	sub.b @13, #55
	ret

; @11 for max length
getHex:
	move.w @12, @0
getHexLoop:
	cmp.w @14, @7
	jz getHexEnd ; over
	move.w @13, @0
	ld.b @13, $00000, @14
	add.w @14, #1
	jsr isHex
	cmp.b @13, #255
	jz getHexEnd ; over
getHexSet:
	lsl.w @12, #4
	or.w @12, @13
	sub.b @11, #1
	cmp.b @11, @0
	jz getHexEnd
	jmp getHexLoop
getHexEnd:
	sub.w @14, #1
	ret

hexChars: .db "0123456789ABCDEF"
prompt_string: .db "$ ", 0

printMemory:
	; Print out memory available
	move.w @1, @9
	asl.w @1, #3
	move.b @2, #4
	jsr printHex
	move.w @1, #ram_amount_string
	jsr putsF
	ret

char_goto:
	jsr skipSpaces
	move.b @11, #4
	jsr getHex
	move.w @15, #$0080
	jsr $00000, @12
	jmp parse_end

char_write:
	jsr skipSpaces
	move.b @11, #4
	jsr getHex
	move.w @5, @12 ; Set @5 to pos
	jsr skipSpaces
char_write_loop:
	move.b @11, #2
	jsr getHex
	st.b $00000, @5, @12
	add.w @5, #1
	add.w @14, #1
	cmp.w @14, @7
	jz parse_end ; Over
	jmp char_write_loop

try_load_tape:
detect_tape:
	; Start looking
	move.w @10, @0
detect_tape_loop:
	ld.b @12, $FD003, @10
	cmp.b @12, #2
	jz detect_tape_found
	add.w @10, #$0100
	cmp.w @10, #$1000
	jnz detect_tape_loop
	ret
detect_tape_found:
	; @13 - temp
	; @12 - constant dot
	; @11 - length
	; @10 - tape pointer
	cli ; No interrupts, we're going to use polling as it is easier
	move.w @1, #loading
	jsr putsF
	move.w @12, #46
	move.w @11, @0
tape_read:
	; Set seek
	move.w @13, #1
	st.w $FD006, @10, @13
tape_read_draw_dot: ; While reading :3
	move.w @13, @11
	and.w @13, #$000F
	jnz tape_read_loop1 ; No dot
	st.b $FD008, @8, @12
tape_read_loop1:
	nop
	ld.b @13, $FD009, @10
	cmp.b @13, #1
	jz tape_read_loop1 ; Loaded?
tape_read_read:
	ld.w @13, $FD006, @10 ; Check seek amount
	cmp.w @13, #1
	jnz tape_read_end ; Not equal to 1 seek?
	ld.b @13, $FD008, @10
	st.b $00000, @5, @13 ; Write to memory
	add.w @5, #1
	add.w @11, #1
tape_read_finish:
	cmp.w @6, @11
	jnz tape_read ; Read all? If not, jump
tape_read_end:
	move.w @1, #ok_string
	jsr putsF
	jmp parse_end

; Set color
char_color:
	jsr skipSpaces
	move.b @11, #4
	jsr getHex
	st.w $FD010, @8, @12
	jmp parse_end

; @4 - current pos
; @5 - pos
; @6 - length
; @7 - text length
; @8, @9 - reserved
; @10-@13 - tmp
; @14 - text pointer
char_load:
	jsr skipSpaces
	move.b @11, #4
	jsr getHex
	move.w @5, @12
	jsr skipSpaces
	move.b @11, #4
	jsr getHex
	move.w @6, @12
char_load_begin:
	jsr try_load_tape
	move.w @1, #device_not_found
	jsr putsF
	jmp parse_end

char_print:
	move.w @3, @0 ; Reset seg
	move.w @6, #$10 ; Default length
	jsr skipSpaces
	move.b @11, #4
	jsr getHex
	move.w @5, @12 ; Set @5 to pos
	move.w @12, @7
	sub.w @12, #1
	cmp.w @14, @12
	jnc char_print_fin ; No seg or len
	cmp.w @11, @0
	jnz char_print_noSegment ; Did not read all 4
	; Check for segment
	add.w @14, #1
	ld.b @13, $00000, @14
	jsr isHex
	cmp.b @13, #255
	jz char_print_noSegment
char_print_segment:
	move.w @12, @5
	lsl.w @5, #4
	or.w @5, @13 ; @5 now stores low 16 bits, @12 stores high 16 bits
	lsr.w @12, #12 ; @12 now stores segment
	move.w @3, @12
char_print_noSegment:
	move.w @12, @7
	sub.w @12, #1
	cmp.w @14, @12
	jnc char_print_fin ; No seg or len
	jsr skipSpaces
	move.b @11, #4
	jsr getHex
	move.w @6, @12 ; Set @6 to len
	cmp.w @6, #$81
	jc char_print_fin ; Too large?
	move.w @6, #$80
char_print_fin:
	cmp.w @6, #0
	jz parse_end
	move.w @1, @3
	lsl.w @1, #12
	move.w @2, #1
	jsr printHex
	move.w @1, @5
	move.w @2, #4
	jsr printHex
	move.w @1, #print_sep_string
	jsr putsF
	move.w @10, #4
char_print_loop:
	cmp.w @6, #0
	jz parse_end
	move.w @1, #32
	st.b $FD008, @8, @1
	move.w @12, @3
	asl.w @12, #2 ; multiply by 5
	add.w @12, @3
	add.w @12, #load_table
	jsr $F0000, @12
	lsl.w @1, #8
	move.w @2, #2
	jsr printHex
	add.w @5, #1
	sub.w @6, #1
	sub.w @10, #1
	cmp.w @10, #0
	jnz char_print_loop
	cmp.w @6, #0
	jz parse_end
	st.b $FD00E, @8, @0
	jmp char_print_fin

char_info:
	jsr printMemory
	st.b $FD00E, @8, @0
	move.w @1, #sp_string
	jsr putsF
	move.w @2, #4
	move.w @1, @15
	jsr printHex
	jmp parse_end

char_enter: ; Parse!
	move.w @1, #$06
	st.w $FD004, @8, @1
	move.w @15, #$0080
	move.w @14, #2 ; Pointer
	ld.b @1, $00002
	cmp.b @1, #97
	jc char_parse
	sub.b @1, #32
char_parse:
	cmp.b @1, #80
	jz char_print
	cmp.b @1, #87
	jz char_write
	cmp.b @1, #73
	jz char_info
	cmp.b @1, #71
	jz char_goto
	cmp.b @1, #76
	jz char_load
	cmp.b @1, #67
	jz char_color

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
	ret

int_vec_char:
	ld.w @1, $FD00C, @8
	cmp.w @1, #0
	jz int_vec_end
	jsr int_vec_char_read
	jmp int_vec_char

int_vec_char_read:
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
	jnc char_range2
	ret
char_range2:
	cmp.w @1, #127
	jc char_add
	ret
char_add: ; Whee
	st.b $00000, @7, @1
	add.w @7, #1
	ret

int_vec:
	cli
	; Stash some registers on the stack.
	sub.w @15, #2
	st.w $00000, @1
	sub.w @15, #2
	st.w $00000, @12
	sub.w @15, #2
	st.w $00000, @13

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
	ld.w @13, $00000
	add.w @15, #2
	ld.w @12, $00000
	add.w @15, #2
	ld.w @1, $00000
	add.w @15, #2

	; Return.
	popf
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
		st.b $FD008, @8, @1
		jmp putsF_lp1
putsF_ret:
	ret

load_table:
	ld.b @1, $00000, @5
	ret
	ld.b @1, $10000, @5
	ret
	ld.b @1, $20000, @5
	ret
	ld.b @1, $30000, @5
	ret
	ld.b @1, $40000, @5
	ret
	ld.b @1, $50000, @5
	ret
	ld.b @1, $60000, @5
	ret
	ld.b @1, $70000, @5
	ret
	ld.b @1, $80000, @5
	ret
	ld.b @1, $90000, @5
	ret
	ld.b @1, $A0000, @5
	ret
	ld.b @1, $B0000, @5
	ret
	ld.b @1, $C0000, @5
	ret
	ld.b @1, $D0000, @5
	ret
	ld.b @1, $E0000, @5
	ret
	ld.b @1, $F0000, @5
	ret

no_ram_string:
	.db "NO RAM FOUND", 0
ram_amount_string:
	.db "0 BYTES "
ok_string:
	.db "OK", 0
sp_string:
	.db "SP: F", 0
print_sep_string:
	.db " |", 0
device_not_found:
	.db "DEVICE NOT FOUND", 0
loading:
	.db "LOADING", 0

vec_cli_idle:
	.dw halt
	.db $0F

	.org $FFF80
code_end:
	.dw $0000 ; version identifier
	.db "Areia-1 BIOS ROM", $00

	.org $FFFFC
	.dd code_start

