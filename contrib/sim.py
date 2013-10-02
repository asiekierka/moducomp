# Areia-1 CPU simulator
# by Ben "GreaseMonkey" Russell, 2013
# Licence: CC0: http://creativecommons.org/publicdomain/zero/1.0/

import sys, struct, time

SHADOW_ENABLED = True
SHADOW_PREFILL = False

TRIG_RECALC = -3
TRIG_DYNA = -2
TRIG_OPEN = -1

F_ZERO = 0x0001
F_CARRY = 0x0002
F_OVER = 0x0004
F_SIGN = 0x0008
F_INT = 0x0010

def ispowerof2(v):
	nv = v

	nv = (nv>>16) | nv
	nv = (nv>>8)  | nv
	nv = (nv>>4)  | nv
	nv = (nv>>2)  | nv
	nv = (nv>>1)  | nv

	return nv == v*2-1

def log2(v):
	n = 0
	while v != 0:
		n += 1
		v >>= 1

	return v

class Memory:
	def get_shadow_size(self):
		return 0
	
	def get_shadow_address(self, addr):
		return TRIG_DYNA
	
	def read8(self, addr):
		return 0, 0xFF

	def write8(self, cpu, addr, val):
		return 0

class StringROM(Memory):
	def __init__(self, size, data):
		assert len(data) <= size
		assert size >= 0x00001
		assert size <= 0x10000
		assert ispowerof2(size)
		self.size = size
		self.data = data
		data += "\xFF"*(self.size-len(self.data))
		self.confspace = "\xDF\x99\x00\xFF" + chr(0x00 + log2(size)) + "\xFF"*3
		assert ispowerof2(len(self.confspace))

	def get_shadow_size(self):
		return self.size + 256

	def get_shadow_address(self, addr):
		if addr & 0x200000:
			return self.size + (addr&255)
		else:
			return addr & (self.size-1)
	
	def read8(self, addr):
		if addr & 0x200000:
			return 0, ord(self.confspace[addr & (len(self.confspace)-1)])
		else:
			return 0, ord(self.data[addr & (self.size-1)])

class RAM(Memory):
	def __init__(self, size):
		assert size >= 0x00001
		assert size <= 0x10000
		assert ispowerof2(size)
		self.size = size
		self.data = [0xFF for i in xrange(size)]
		self.confspace = "\xDF\x99\x00\xFF" + chr(0x10 + log2(size)) + "\xFF"*3
		assert ispowerof2(len(self.confspace))

	def get_shadow_size(self):
		return self.size + 256
	
	def get_shadow_address(self, addr):
		if addr & 0x200000:
			return self.size + (addr&255)
		else:
			return addr & (self.size-1)

	def read8(self, addr):
		if addr & 0x200000:
			return 0, ord(self.confspace[addr & (len(self.confspace)-1)])
		else:
			return 0, self.data[addr & (self.size-1)]

	def write8(self, cpu, addr, val):
		if not (addr & 0x200000):
			self.data[addr & (self.size-1)] = val
			return 0
		else:
			return 0

class DebugSysSlot(Memory):
	def read8(self, addr):
		if addr & 0x200000:
			addr &= 0xff
			if addr == 0xfe:
				return 0, 0xff
		return 0, 0xff

	def write8(self, cpu, addr, val):
		if addr & 0x200000:
			addr &= 0xff
			if addr == 0xfe:
				sys.stdout.write(chr(val) if val <= 0x7F else ".")
				sys.stdout.flush()
				return 0
		return 0

class StandardMemoryController(Memory):
	def __init__(self):
		self.slots = [None for i in xrange(15)]
		self.saddr_slots = [0 for i in xrange(15)]
		self.saddr_rom = 0
		self.saddr_sys = 0
		self.rom = None
		self.sys_slot = None
	
	def shadow_update(self):
		offs = 0
		for i in xrange(15):
			self.saddr_slots[i] = offs
			if self.slots[i] != None:
				offs += self.slots[i].get_shadow_size()
		self.saddr_rom = offs
		if self.rom != None:
			offs += self.rom.get_shadow_size()
		self.saddr_sys = offs

	def set_slot(self, bank, slot):
		self.slots[bank] = slot
		self.shadow_update()

	def set_rom(self, rom):
		self.rom = rom
		self.shadow_update()

	def set_sys_slot(self, slot):
		self.sys_slot = slot
		self.shadow_update()

	def route(self, addr, fn, *args):
		addr &= 0xFFFFF
		bank1 = addr>>16
		slot = None
		if bank1 == 0xF:
			lower = addr & 0xFFFF
			if lower < 0xBFFF:
				# 48KB mirror
				slot = self.slots[0]
				addr &= 0xFFFF
			elif lower < 0xCFFF:
				# Reserved
				pass
			elif lower < 0xDFFF:
				# I/O region
				bank2 = (addr>>8) & 0xF
				if bank2 == 0xF:
					slot = self.sys_slot
				else:
					slot = self.slots[bank2]
				addr = (addr & 0xFF) | 0x200000
			else:
				# ROM
				slot = self.rom
		else:
			slot = self.slots[bank1]
			addr &= 0xFFFF

		return fn(slot, addr, *args)

	def get_shadow_size(self):
		return (sum(o.get_shadow_size() if o != None else 0 for o in self.slots) + 
			(self.rom.get_shadow_size() if self.rom != None else 0) +
			256)
		
	def get_shadow_address(self, addr):
		bank = addr>>16
		if addr >= 0xF0000 and addr <= 0xFBFFF:
			# 48KB mirror
			bank = 0

		if bank == 0xF:
			bsel = (addr>>12)&15
			if bsel == 0xC:
				return TRIG_OPEN
			elif bsel == 0xD:
				subbank = (addr>>8)&15
				if subbank == 0xF:
					if self.sys_slot == None:
						return TRIG_OPEN
					else:
						offs = self.saddr_sys
						ra = self.sys_slot.get_shadow_address((addr & 0xFF) | 0x200000)
				elif self.slots[subbank] == None:
					return TRIG_OPEN
				else:
					offs = self.saddr_slots[subbank]
					ra = self.slots[subbank].get_shadow_address((addr & 0xFF) | 0x200000)
			elif self.rom == None:
				return TRIG_OPEN
			else:
				assert bsel >= 0xE and bsel <= 0xF
				offs = self.saddr_rom
				#print offs, self.rom.get_shadow_size()
				ra = self.rom.get_shadow_address((addr & 0x1FFF))
		elif self.slots[bank] == None:
			return TRIG_OPEN
		else:
			offs = self.saddr_slots[bank]
			ra = self.slots[bank].get_shadow_address(addr & 0xFFFF)
		
		if ra >= 0:
			return offs + ra
		else:
			return ra

	def read8(self, addr):
		return self.route(addr, lambda slot, addr: slot.read8(addr) if slot != None else (0, 0xFF))

	def write8(self, cpu, addr, val):
		return self.route(addr, lambda slot, addr, cpu, val: slot.write8(cpu, addr, val) if slot != None else 0, cpu, val)

class CPU:
	def __init__(self, memctl):
		self.memctl = memctl
		self.reset_shadow()
		self.regs = [0 for i in xrange(16)]
		self.cfg = [0 for i in xrange(128)]
		self.cycles = 0
		self.cold_reset()
		self.cycles = 0
		self.halted = True
		self.needs_full_reset = True
		self.reset_shadow()
		self.reset_jit()
	
	def reset_jit(self):
		self.jit_closure = [None for i in xrange(1<<20)]

	def reset_shadow(self):
		self.shadow_count = self.memctl.get_shadow_size() + 128
		self.shadow_addr = [TRIG_RECALC for i in xrange(1<<20)] # int
		self.shadow_data = [0 for i in xrange(self.shadow_count)] # unsigned byte
		self.shadow_mask = [0 for i in xrange((self.shadow_count+31)>>5)] # int bitmask
		self.shadow_cyc = [0 for i in xrange(self.shadow_count)] # int?
		print "shadow reset, %i bytes" % (self.shadow_count)

	def deshadow(self, addr):
		saddr = self.shadow_addr[addr]
		if saddr >= 0:
			self.shadow_mask[saddr>>5] &= ~(1<<(saddr&31))

	def readcfg(self, addr):
		return 0, self.cfg[addr & 0x7F]

	def writecfg(self, addr, val):
		assert (val&~0xFF) == 0
		self.cfg[addr & 0x7F] = val
		return 0

	def write8(self, addr, val):
		assert (val&~0xFF) == 0

		if addr < 0xFFF80:
			cyc = self.memctl.write8(self, addr, val)
		else:
			cyc = self.writecfg(addr & 0x7F, val)

		if SHADOW_ENABLED:
			saddr = self.shadow_addr[addr]
			if saddr >= 0:
				self.shadow_mask[saddr>>5] &= ~(1<<(saddr&31))

		self.cycles += cyc+1

	def write16(self, addr, val):
		v0 = self.write8(addr, (val & 0xFF))
		v1 = self.write8(addr+1, (val >> 8) & 0xFF)

	def read16(self, addr):
		v0 = self.read8(addr)
		v1 = self.read8(addr+1)
		val = v0 | (v1<<8)
		return val

	def read8(self, addr):
		if SHADOW_ENABLED:
			saddr = self.shadow_addr[addr]
			if saddr >= 0:
				if (self.shadow_mask[saddr>>5] & (1<<(saddr&31))) != 0:
					self.cycles += self.shadow_cyc[saddr]+1
					return self.shadow_data[saddr]
			elif saddr == TRIG_OPEN:
				self.cycles += 1
				return 0xFF

		if self.needs_full_reset or addr < 0xFFF80:
			cyc, val = self.memctl.read8(addr)
			if SHADOW_ENABLED:
				saddr = self.memctl.get_shadow_address(addr)
		else:
			cyc, val = self.readcfg(addr & 0x7F)
			if SHADOW_ENABLED:
				saddr = self.shadow_count - 128 + (addr & 127)

		self.cycles += cyc+1

		assert (val&~0xFF) == 0

		if SHADOW_ENABLED:
			self.shadow_addr[addr] = saddr
			if saddr >= 0:
				self.shadow_cyc[saddr] = cyc
				self.shadow_data[saddr] = val
				self.shadow_mask[saddr>>5] |= (1<<(saddr&31))
			elif saddr == TRIG_OPEN:
				self.shadow_mask[saddr>>5] |= (1<<(saddr&31))

		return val

	def fetch8(self):
		val = self.read8(self.pc)
		self.pc = (self.pc + 1) & 0xFFFFF
		return val

	def fetch16(self):
		val = self.read16(self.pc)
		self.pc = (self.pc + 2) & 0xFFFFF
		return val

	def cold_reset(self):
		self.needs_full_reset = True
		self.pc = 0xFFFFC
		pc_low = self.fetch16()
		pc_high = self.fetch8()
		self.writecfg(0xFFFFFC, pc_low&0xFF)
		self.writecfg(0xFFFFFD, pc_low>>8)
		self.writecfg(0xFFFFFE, pc_high)
		self.pc = (pc_low | (pc_high << 16)) & 0xFFFFF
		print "Hard Reset PC: %05X" % self.pc
		self.needs_full_reset = False

		self.warm_reset()

	def warm_reset(self):
		assert not self.needs_full_reset
		for i in xrange(16):
			self.regs[i] = 0
		self.flags = 0
		self.pc = 0xFFFFC
		pc_low = self.fetch16()
		pc_high = self.fetch8()
		self.pc = (pc_low | (pc_high << 16)) & 0xFFFFF
		print "Soft Reset PC: %05X" % self.pc
		self.halted = False

	def flag_set(self, mask, v):
		if v:
			self.flags |= mask
		else:
			self.flags &= ~mask

	def flag_zero(self, v):
		self.flag_set(F_ZERO, v == 0)

	def flag_parity(self, v):
		v &= 0xFFFF
		v = ((v&0xAAAA)>>1)+(v&0x5555)
		v = ((v&0xCCCC)>>2)+(v&0x3333)
		v = ((v&0xF0F0)>>4)+(v&0x0F0F)
		v = ((v&0xFF00)>>8)+(v&0x00FF)
		self.flag_set(F_OVER, (v & 1) != 0)

	def flag_sign(self, v):
		self.flag_set(F_SIGN, v != 0)
	
	def do_op1(self, size, op, reg_x, imm, imm_is_y):
		if imm_is_y:
			if size:
				def ret_0():
					return self.regs[imm] & 0xFFFF
			else:
				def ret_0():
					return self.regs[imm] & 0xFF
		else:
			if size:
				def ret_0():
					return imm & 0xFFFF
			else:
				def ret_0():
					return imm & 0xFF

		assert reg_x != 0

		if op == 0x0:
			# MOVE
			def ret(imm):
				if size:
					self.regs[reg_x] = imm
				else:
					self.regs[reg_x] = (self.regs[reg_x] & 0xFF00) | imm

			return lambda : ret(ret_0())
		elif op >= 0x1 and op <= 0x3:
			def ret_1():
				if size:
					xval = self.regs[reg_x] & 0xFFFF
				else:
					xval = self.regs[reg_x] & 0xFF

				return xval

			# TODO: overflow flag!
			if op == 0x2:
				def ret_2(xval, imm):
					# ADD
					if size:
						self.flag_set(F_CARRY, xval + imm >= 0x10000)
					else:
						self.flag_set(F_CARRY, xval + imm >= 0x100)

					retval = xval + imm

					if ((xval ^ imm) & 0x8000) == 0:
						# same signs
						aimm = (imm if imm < 0x8000 else 0x10000-imm)
						axval = (xval if xval < 0x8000 else 0x10000-xval)
						if size:
							self.flag_set(F_OVER, aimm + axval >= 0x8000)
						else:
							self.flag_set(F_OVER, aimm + axval >= 0x80)
					else:
						# differing signs
						self.flag_set(F_OVER, False)

					return retval
			else:
				def ret_2(xval, imm):
					# CMP/SUB
					self.flag_set(F_CARRY, xval < imm) 
					retval = xval - imm

					if ((xval ^ imm) & 0x8000) != 0:
						# differing signs
						aimm = (imm if imm < 0x8000 else 0x10000-imm)
						axval = (xval if xval < 0x8000 else 0x10000-xval)
						if size:
							self.flag_set(F_OVER, aimm + axval >= 0x8000)
						else:
							self.flag_set(F_OVER, aimm + axval >= 0x80)
					else:
						# same signs
						self.flag_set(F_OVER, False)

					return retval

			if size:
				def ret_3(retval):
					retval &= 0xFFFF
					if op != 0x1:
						self.regs[reg_x] = retval
					self.flag_zero(retval & 0xFFFF)
					self.flag_sign(retval & 0x8000)
			else:
				def ret_3(retval):
					retval &= 0xFF
					if op != 0x1:
						self.regs[reg_x] = (self.regs[reg_x] & 0xFF00) | retval
					self.flag_zero(retval & 0xFF)
					self.flag_sign(retval & 0x80)

			return lambda : ret_3(ret_2(ret_1(), ret_0()))
		elif op >= 0x4 and op <= 0x6:
			if op == 0x4:
				# XOR
				def ret_1(imm):
					self.regs[reg_x] ^= imm
			elif op == 0x5:
				# OR
				def ret_1(imm):
					self.regs[reg_x] |= imm
			elif op == 0x6:
				# AND
				if not size:
					def ret_1(imm):
						immx = imm | 0xFF00
						self.regs[reg_x] &= immx
				else:
					def ret_1(imm):
						self.regs[reg_x] &= imm

			if size:
				def ret_2(_):
					self.flag_zero(self.regs[reg_x] & 0xFFFF)
					self.flag_parity(self.regs[reg_x] & 0xFFFF)
					self.flag_sign(self.regs[reg_x] & 0x8000)
			else:
				def ret_2(_):
					self.flag_zero(self.regs[reg_x] & 0xFF)
					self.flag_parity(self.regs[reg_x] & 0xFFFF)
					self.flag_sign(self.regs[reg_x] & 0x80)

			return lambda : ret_2(ret_1(ret_0()))

	def do_op2(self, imm_isnt_y, op, reg_x, imm):
		#print op, reg_x, imm
		if not imm_isnt_y:
			def ret_0():
				return self.regs[imm] & 0x0F
		else:
			def ret_0():
				return imm

		if op == 0x0:
			# ASL/LSL
			def ret_1(imm):
				xval = self.regs[reg_x]
				self.flag_set(F_CARRY, (xval & (0x10000>>imm)) != 0)
				xval = (xval<<imm) & 0xFFFF
				return xval
		elif op == 0x1 or op == 0x2:
			# ASR, LSR
			def ret_1(imm):
				xval = self.regs[reg_x]
				self.flag_set(F_CARRY, (xval & 0x0001) != 0)
				if op == 0x1 and (xval & 0x8000) != 0:	
					xval = ((xval >> imm) | (((1<<imm)-1) << (15-imm))) & 0xFFFF
				else:
					xval >>= imm
				return xval
		elif op == 0x3:
			# ROL
			def ret_1(imm):
				xval = self.regs[reg_x]
				for i in xrange(imm):
					rotbit = xval>>15
					xval = (xval << 1) | (rotbit)
					self.flag_set(F_CARRY, rotbit != 0)
				return xval
		elif op == 0x4:
			# ROR
			def ret_1(imm):
				xval = self.regs[reg_x]
				for i in xrange(imm):
					rotbit = xval&1
					xval = (xval >> 1) | (rotbit << 15)
					self.flag_set(F_CARRY, rotbit != 0)
				return xval
		elif op == 0x5:
			# RCL
			def ret_1(imm):
				xval = self.regs[reg_x]
				for i in xrange(imm):
					rotbit = (1 if (self.flags & F_CARRY) else 0)
					xval = (xval << 1) | (rotbit)
					self.flag_set(F_CARRY, rotbit != 0)
				return xval
		elif op == 0x6:
			# RCR
			def ret_1(imm):
				xval = self.regs[reg_x]
				for i in xrange(imm):
					rotbit = (1 if (self.flags & F_CARRY) else 0)
					xval = (xval >> 1) | (rotbit << 15)
					self.flag_set(F_CARRY, rotbit != 0)
				return xval
		else:
			assert False

		def ret_2(xval):
			self.flag_zero(xval)
			self.flag_parity(xval)
			self.flag_sign(xval)
			self.regs[reg_x] = xval

		return lambda : ret_2(ret_1(ret_0()))
	
	def do_cycle(self):
		if self.halted:
			self.cycles += 1
			return

		jc = self.jit_closure[self.pc]
		if jc != None:
			size, cycles, exit_pc, f = jc

			# check region for modifications
			# TODO! (for now, let's just assume there's no self-modifying code)
			
			self.pc += size
			self.cycles += cycles

			f()
		else:
			self.make_closure_and_run_it()
	
	def make_closure_and_run_it(self):
		pc = self.pc
		val = self.read8(self.pc)
		ocyc = self.cycles
		op = self.parse_cycle()
		ncyc = self.cycles
		npc = self.pc
		op()
		xpc = self.pc
		self.jit_closure[pc] = (npc - pc, ncyc - ocyc, xpc, op)

	def parse_cycle(self):
		if self.halted:
			self.cycles += 1
			return lambda : None

		op = self.fetch8()

		#print "%05X: %02X" % (self.pc-1, op)
		#print " ".join("%04X" % v for v in self.regs)

		if (op & 0x0F) == 0 and (op & 0xF0) != 0xF0:
			op >>= 4
			if op == 0x00:
				# NOP
				def ret():
					pass
			elif op == 0x02:
				# RET
				def ret():
					pc_low = self.read16(self.regs[15] | 0xF0000)
					self.regs[15] += 2
					pc_high = self.read8(self.regs[15] | 0xF0000)
					self.regs[15] += 1
					self.pc = (pc_low | (pc_high << 16)) & 0xFFFFF
			elif op == 0x04:
				# POPF
				def ret():
					self.flags = self.read16(self.regs[15] | 0xF0000)
					self.regs[15] += 2
			elif op == 0x06:
				# PUSHF
				def ret():
					self.regs[15] -= 2
					self.write16(self.regs[15] | 0xF0000, self.flags)
			elif op == 0x08:
				# CLI
				def ret():
					self.flags &= ~F_INT
			elif op == 0x0A:
				# SEI
				def ret():
					self.flags |= F_INT
			elif op == 0x0C:
				# HLT
				def ret():
					self.halted = True
			else:
				assert False

			return ret

		elif (op & 0xE0) == 0xE0:
			subop = op & 7
			if subop == 7:
				# OP3 / OP4
				op2 = self.fetch8()
				reg_x = op2 & 0x0F
				op2 >>= 4

				if op & 0x08:
					# OP4
					is_store = (op2 & 1) != 0
					size = (op & 0x10)
					op2 >>= 1
					reg_y_data = 0
					if op2 == 1:
						# $Faaaa
						imm = 0xF0000 + self.fetch16()
					elif op2 == 2:
						# $baa00, @y
						op_yb = self.fetch8()
						reg_y = (op_yb>>4)
						op_yb &= 0x0F

						reg_y_data = reg_y
						imm = (op_yb<<16) + (self.fetch8()<<8)
					elif op2 == 3:
						# $baaaa, @y
						op_yb = self.fetch8()
						reg_y = (op_yb>>4)
						op_yb &= 0x0F

						reg_y_data = reg_y
						imm = (op_yb<<16) + self.fetch16()
					else:
						assert False

					if is_store:
						# ST
						if size:
							def ret():
								self.write16(imm + self.regs[reg_y_data], self.regs[reg_x])
						else:
							def ret():
								self.write8(imm + self.regs[reg_y_data], self.regs[reg_x] & 0xFF)
					else:
						# LD
						if size:
							if reg_x != 0:
								def ret():
									val = self.read16(imm + self.regs[reg_y_data])
									self.regs[reg_x] = val
							else:
								def ret():
									val = self.read16(imm + self.regs[reg_y_data])
						else:
							if reg_x != 0:
								def ret():
									val = self.read8(imm + self.regs[reg_y_data])
									self.regs[reg_x] = (self.regs[reg_x] & 0xFF00) | val
							else:
								def ret():
									val = self.read8(imm + self.regs[reg_y_data])

					return ret

				else:
					# OP3
					new_reg_x = 0
					if op & 0x10:
						# OP3 imm20
						new_pc = self.fetch16() | (reg_x<<16)
					else:
						# OP3 reg imm8
						new_pc = (self.fetch8()<<12)
						new_reg_x = reg_x

					def ret_1():
						return (new_pc + self.regs[new_reg_x]) & 0xFFFFF

					if op2 == 0x0: # JZ
						def ret_2(new_pc):
							if self.flags & F_ZERO:
								self.pc = new_pc
					elif op2 == 0x1: # JNZ
						def ret_2(new_pc):
							if not (self.flags & F_ZERO):
								self.pc = new_pc
					elif op2 == 0x2: # JC
						def ret_2(new_pc):
							if self.flags & F_CARRY:
								self.pc = new_pc
					elif op2 == 0x3: # JNC
						def ret_2(new_pc):
							if not (self.flags & F_CARRY):
								self.pc = new_pc
					elif op2 == 0x4: # JV
						def ret_2(new_pc):
							if self.flags & F_OVER:
								self.pc = new_pc
					elif op2 == 0x5: # JNV
						def ret_2(new_pc):
							if not (self.flags & F_OVER):
								self.pc = new_pc
					elif op2 == 0x6: # JS
						def ret_2(new_pc):
							if self.flags & F_SIGN:
								self.pc = new_pc
					elif op2 == 0x7: # JNS
						def ret_2(new_pc):
							if not (self.flags & F_SIGN):
								self.pc = new_pc
					elif op2 == 0x8: # JMP
						def ret_2(new_pc):
							self.pc = new_pc
					elif op2 == 0x9: # JSR
						def ret_2(new_pc):
							pc_low = self.pc & 0xFFFF
							pc_high = (self.pc >> 16) & 0x0F
							self.regs[15] -= 1
							self.write8(self.regs[15] | 0xF0000, pc_high)
							self.regs[15] -= 2
							self.write16(self.regs[15] | 0xF0000, pc_low)
							self.pc = new_pc
					else:
						assert False

					return lambda : ret_2(ret_1())
			else:
				# OP1 reg reg / OP2
				size = (op & 0x10) != 0
				op2 = self.fetch8()
				reg_x = (op2 & 0x0F)
				reg_y = (op2 >> 4)

				if op & 0x08:
					return self.do_op2(size, subop, reg_x, reg_y)
				else:
					return self.do_op1(size, subop, reg_x, reg_y, True)
		else:
			# OP1
			size = (op & 0x10) != 0
			reg_x = op & 0x0F
			op >>= 5

			if size:
				imm = self.fetch16()
			else:
				imm = self.fetch8()

			return self.do_op1(size, op, reg_x, imm, False)

	def run_until_halt(self):
		while not self.halted:
			self.do_cycle()

for reps in xrange(1):
	memctl = StandardMemoryController()
	memctl.set_rom(StringROM(8<<10, open("bios.rom", "rb").read()))
	memctl.set_slot(0, RAM(4096))
	memctl.set_sys_slot(DebugSysSlot())
	memctl.shadow_update()
	cpu = CPU(memctl)
	print
	print "run #%i" % (reps+1,)
	cpu.cold_reset()
	if SHADOW_ENABLED and SHADOW_PREFILL:
		print "filling shadow"
		for i in xrange(1<<20):
			cpu.read8(i)
	print "running until halt"
	t_start = time.time()
	cpu.cycles = 0
	cpu.run_until_halt()
	t_end = time.time()
	print "HALT: %05X: " % (cpu.pc-1,)
	print " ".join("%04X" % v for v in cpu.regs)
	t = t_end - t_start
	print "time taken: %f seconds" % (t,)
	print "cycles: %i" % (cpu.cycles)
	print "rate: %.6f MHz" % (cpu.cycles / (t * 10**6))

