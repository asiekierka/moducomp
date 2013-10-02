; Code by GreaseMonkey

	.org $FE000
crc_base:
	.dw $5BCE
code_start:
	; Clear interrupts
	cli

	; Check if we have RAM in the "low" 256 bytes
	xor.w @2, @2
	lp_ckram1:
		; Write RAM to $00000
		st.b $00000, @2, @2
		add.b @2, #1
		cmp.b @2, #$80
		jnz lp_ckram1
		move.b @2, #$00
	lp_ckram2:
		; Read RAM from $F0000 and compare (1. we need this alias, and 2. we don't want any "cache" to screw us over should we ever add one)
		ld.b @1, $F0000, @2
		cmp.b @1, @2
		jnz lpf_ckram
		add.b @2, #1
		cmp.b @2, #$80
		jnz lp_ckram2
		jmp lpx_ckram_ok
	lpf_ckram:
		; Welp. We can't put a stack anywhere.
		; We have to use this nasty hack.
		move.w @1, #str_ram0_fail
		move.w @15, #vec_cli_idle
		jmp putsF
	lpx_ckram_ok:

	; Set stack pointer
	move.w @15, #$0080

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

	move.w @1, #str_crc_testing
	jsr putsF
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
	ld.w @2, crc_base
	move.w @13, @1
	move.w @14, @2
	xor.w @1, @2
	jz crc_pass
		move.w @1, #str_fail
		jsr putsF
		jmp cli_idle
crc_pass:
	move.w @1, #str_ok
	jsr putsF

	jmp cli_idle

cli_idle:
	cli
idle:
	hlt
	jmp idle

	; INPUT:
	;   @1 = input string in $Fxxxx bank
	; CLOBBERS: @1, @2
putsF:
	move.w @2, @1
putsF_lp1:
	ld.b @1, $F0000, @2
	and.b @1, @1
	jz putsF_ret
		add.w @2, #1
		st.b $FDFFE, @1
		jmp putsF_lp1
putsF_ret:
	ret

str_ram0_fail: .db "ERROR: No RAM in slot 0\n", 0
str_crc_testing: .db "Testing CRC16...", 0
str_ok: .db "OK\n", 0
str_fail: .db "FAIL\n", 0

vec_cli_idle:
	.dw cli_idle
	.db $0F

	.org $FFF80
code_end:
	.dw $0000 ; version identifier
	.db "Areia-1 BIOS ROM", $00

	.org $FFFFC
	.dd code_start

