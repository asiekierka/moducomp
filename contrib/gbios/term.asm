
	; ASSUMES: seg2 == $F0
	; INPUT: none
	; OUTPUT:
	; - @1.w = offset for $FD000
	; CLOBBERS: @2.b
term_find:
	move.b @1, #$F0
	move.w @1, @0
	term_find_lp:
		ld.b:2 @2, $FD003, @1
		cmp.b @2, #$01
		jz term_find_ret
		add.w @1, #$0100
		cmp.w @1, #$1000
		jnz term_find_lp

	; could not find terminal - HALT
	cli
	hlt

	; INPUT:
	; - @1.b = char to write
	; - @14.w:2 = monitor I/O offset
	; OUTPUT: none
	; CLOBBERS: none
term_putc:
	st.b:2 $FD008, @14, @1
	ret

	; INPUT:
	; - @1.w:1 = address of string
	; - @14.w:2 = monitor I/O offset
	; OUTPUT: none
	; CLOBBERS: @1.w, @2.w
term_puts:
	move.w @2, @1
	term_puts_lp:
		ld.b:1 @1, @2
		cmp.b @1, #$00
		jz term_puts_lpx

		; TODO: control characters

		jsr term_putc
		jmp term_puts_lp
	term_puts_lpx:

	ret
	

