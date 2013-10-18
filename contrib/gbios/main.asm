	.org $FE000
code_start:
start:
	; The usual setup.
	cli
	move.b @1, #$F0
	sseg.0 @0
	sseg.1 @1
	sseg.2 @1
	sseg.3 @0

	; Find a terminal we can use.
	jmp term_find
term_find_ret:
	move.w @14, @1

	move.w @1, #str_intro
	jsr term_puts

	.include "gbios/term.asm"

str_intro:
	.db "gbios - (C) MegaCorp 20xx", 0

code_end:
	.org $FFFFC
	.dd start

