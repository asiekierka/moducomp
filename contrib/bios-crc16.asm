; Code by GreaseMonkey

	.org $FE000
crc_base:
	.dw $E77D
code_start:
	; Clear interrupts, set up stack.
	cli
	move.w @15, #$0100 ; Assume 256 bytes minimum

	; Set up our interrupt vector.
	move.w @1, #int_vec
	st.w $FFF80, @1
	move.b @1, #$0F
	st.b $FFF82, @1

	; Jump to our interesting code.
	jmp start

int_vec:
	; Stash some registers on the stack.
	sub.w @15, #2
	st.w $00000, @1

	; Clear all interrupts because we are lazy.
	move.w @1, #$FFFF
	st.w $FFF84, @1
	st.w $FFF86, @1

	; Restore registers.
	ld.w @1, $00000
	add.w @15, #2

	; Return.
	popf
	ret

start:
	; Let's do a checksum!
	; None of this one's complement crap, let's do some serious stuff.
	; @1 = CRC16 value
	; @2 = address
	; @3 = in-byte counter
	; @4 = a byte we are reading

	move.w @1, #$FFFF
	move.w @2, #code_start
	crc_lp1:
		; Read a byte and loop through its bits.
		move.b @3, #8
		ld.b @4, $F0000, @2
		crc_lp2:
			; Shift and compare our CRC16.
			lsr.w @1, #1
			jnc crc_lp2_sh1
				xor.w @1, #$8005
			crc_lp2_sh1:

			; Do the same for our input bit.
			lsr.w @4, #1
			jnc crc_lp2_sh2
				xor.w @1, #$8005
			crc_lp2_sh2:

			; Check how many bits we have left.
			sub.b @3, #1
			jnz crc_lp2

		; Advance and check.
		add.w @2, #1
		cmp.w @2, #code_end
		jnz crc_lp1
	
	; check CRC
	ld.w @2, $FE000 ; labels don't work in ld/st in the assembler yet - FIX THIS
	xor.w @1, @2
	
idle:
	cli
	hlt
	; This code should halt and never get an interrupt.
code_end:

	.org $FFF80
	.dw $0000 ; version identifier
	.db "Areia-1 BIOS ROM", $00

	.org $FFFFC
	.dd code_start

