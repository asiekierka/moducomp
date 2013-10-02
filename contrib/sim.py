# Areia-1 CPU simulator
# by Ben "GreaseMonkey" Russell, 2013
# Licence: CC0: http://creativecommons.org/publicdomain/zero/1.0/

import sys, struct, time

SHADOW_ENABLED = True

TRIG_RECALC = -3
TRIG_DYNA = -2
TRIG_OPEN = -1
TRIG_FAIL = None

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
	
	def read8(self, addr, trigger):
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
	
	def read8(self, addr, trigger):
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

	def read8(self, addr, trigger):
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
	def read8(self, addr, trigger):
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
						return offs + self.sys_slot.get_shadow_address((addr & 0xFF) | 0x200000)
				elif self.slots[subbank] == None:
					return TRIG_OPEN
				else:
					offs = self.saddr_slots[subbank]
					return offs + self.slots[subbank].get_shadow_address((addr & 0xFF) | 0x200000)
			elif self.rom == None:
				return TRIG_OPEN
			else:
				assert bsel >= 0xE and bsel <= 0xF
				offs = self.saddr_rom
				#print offs, self.rom.get_shadow_size()
				return offs + self.rom.get_shadow_address((addr & 0x1FFF))
		elif self.slots[bank] == None:
			return TRIG_OPEN
		else:
			offs = self.saddr_slots[bank]
			return offs + self.slots[bank].get_shadow_address(addr & 0xFFFF)

	def read8(self, addr, trigger):
		return self.route(addr, lambda slot, addr, trigger: slot.read8(addr, trigger) if slot != None else (0, 0xFF), trigger)

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

	def readcfg(self, addr, trigger):
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

	def read16(self, addr, trigger):
		if trigger:
			v = self.read16(addr, False)
			if v != TRIG_FAIL:
				return v
		v0 = self.read8(addr, trigger)
		v1 = self.read8(addr+1, trigger)
		if v0 == TRIG_FAIL or v1 == TRIG_FAIL:
			return TRIG_FAIL
		val = v0 | (v1<<8)
		return val

	def read8(self, addr, trigger):
		if SHADOW_ENABLED:
			if trigger:
				v = self.read8(addr, False)
				if v != TRIG_FAIL:
					return v
			else:
				saddr = self.shadow_addr[addr]
				if saddr >= 0:
					if (self.shadow_mask[saddr>>5] & (1<<(saddr&31))) != 0:
						self.cycles += self.shadow_cyc[saddr]+1
						return self.shadow_data[saddr]
				elif saddr == TRIG_OPEN:
					self.cycles += 1
					return 0xFF

		saddr = TRIG_DYNA
		if self.needs_full_reset or addr < 0xFFF80:
			cyc, val = self.memctl.read8(addr, trigger)
			if SHADOW_ENABLED:
				saddr = self.memctl.get_shadow_address(addr)
		else:
			cyc, val = self.readcfg(addr & 0x7F, trigger)
			if SHADOW_ENABLED:
				saddr = self.shadow_count - 128 + (addr & 127)

		if val == TRIG_FAIL:
			return TRIG_FAIL

		self.cycles += cyc+1

		assert (val&~0xFF) == 0

		if SHADOW_ENABLED and not trigger:
			self.shadow_addr[addr] = saddr
			if saddr >= 0:
				self.shadow_cyc[saddr] = cyc
				self.shadow_data[saddr] = val
				self.shadow_mask[saddr>>5] |= (1<<(saddr&31))
			elif saddr == TRIG_OPEN:
				self.shadow_mask[saddr>>5] |= (1<<(saddr&31))

		return val

	def fetch8(self, trigger):
		val = self.read8(self.pc, trigger)
		if val == TRIG_FAIL:
			return TRIG_FAIL
		self.pc = (self.pc + 1) & 0xFFFFF
		return val

	def fetch16(self, trigger):
		val = self.read16(self.pc, trigger)
		if val == TRIG_FAIL:
			return TRIG_FAIL
		self.pc = (self.pc + 2) & 0xFFFFF
		return val

	def cold_reset(self):
		self.needs_full_reset = True
		self.pc = 0xFFFFC
		pc_low = self.fetch16(True)
		pc_high = self.fetch8(True)
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
		pc_low = self.fetch16(True)
		pc_high = self.fetch8(True)
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

	def do_op1(self, size, op, reg_x, imm):
		if size:
			imm &= 0xFFFF
		else:
			imm &= 0xFF

		assert reg_x != 0

		if op == 0x0:
			# MOVE
			if size:
				self.regs[reg_x] = imm
			else:
				self.regs[reg_x] = (self.regs[reg_x] & 0xFF00) | imm
		elif op >= 0x1 and op <= 0x3:
			if size:
				xval = self.regs[reg_x] & 0xFFFF
			else:
				xval = self.regs[reg_x] & 0xFF

			# TODO: overflow flag!
			if op == 0x2:
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
			else:
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

			if size:
				retval &= 0xFFFF
				if op != 0x1:
					self.regs[reg_x] = retval
				self.flag_zero(retval & 0xFFFF)
				self.flag_sign(retval & 0x8000)
			else:
				retval &= 0xFF
				if op != 0x1:
					self.regs[reg_x] = (self.regs[reg_x] & 0xFF00) | retval
				self.flag_zero(retval & 0xFF)
				self.flag_sign(retval & 0x80)
		elif op >= 0x4 and op <= 0x6:
			if op == 0x4:
				# XOR
				self.regs[reg_x] ^= imm
			elif op == 0x5:
				# OR
				self.regs[reg_x] |= imm
			elif op == 0x6:
				# AND
				if not size:
					imm |= 0xFF00
				self.regs[reg_x] &= imm

			if size:
				self.flag_zero(self.regs[reg_x] & 0xFFFF)
				self.flag_parity(self.regs[reg_x] & 0xFFFF)
				self.flag_sign(self.regs[reg_x] & 0x8000)
			else:
				self.flag_zero(self.regs[reg_x] & 0xFF)
				self.flag_parity(self.regs[reg_x] & 0xFFFF)
				self.flag_sign(self.regs[reg_x] & 0x80)

	def do_op2(self, imm_isnt_y, op, reg_x, imm):
		#print op, reg_x, imm
		if not imm_isnt_y:
			imm = self.regs[imm] & 0x0F

		xval = self.regs[reg_x]

		if op == 0x0:
			# ASL/LSL
			self.flag_set(F_CARRY, (xval & (0x10000>>imm)) != 0)
			xval = (xval<<imm) & 0xFFFF
		elif op == 0x1 or op == 0x2:
			# ASR, LSR
			self.flag_set(F_CARRY, (xval & 0x0001) != 0)
			if op == 0x1 and (xval & 0x8000) != 0:	
				xval = ((xval >> imm) | (((1<<imm)-1) << (15-imm))) & 0xFFFF
			else:
				xval >>= imm
		elif op == 0x3:
			# ROL
			for i in xrange(imm):
				rotbit = xval>>15
				xval = (xval << 1) | (rotbit)
				self.flag_set(F_CARRY, rotbit != 0)
		elif op == 0x4:
			# ROR
			for i in xrange(imm):
				rotbit = xval&1
				xval = (xval >> 1) | (rotbit << 15)
				self.flag_set(F_CARRY, rotbit != 0)
		elif op == 0x5:
			# RCL
			for i in xrange(imm):
				rotbit = (1 if (self.flags & F_CARRY) else 0)
				xval = (xval << 1) | (rotbit)
				self.flag_set(F_CARRY, rotbit != 0)
		elif op == 0x6:
			# RCR
			for i in xrange(imm):
				rotbit = (1 if (self.flags & F_CARRY) else 0)
				xval = (xval >> 1) | (rotbit << 15)
				self.flag_set(F_CARRY, rotbit != 0)
		else:
			assert False

		self.flag_zero(xval)
		self.flag_sign(xval)
		self.regs[reg_x] = xval

	def do_cycle(self):
		if self.halted:
			self.cycles += 1
			return

		op = self.fetch8(True)

		#print "%05X: %02X" % (self.pc-1, op)
		#print " ".join("%04X" % v for v in self.regs)

		if (op & 0x0F) == 0 and (op & 0xF0) != 0xF0:
			op >>= 4
			if op == 0x00:
				# NOP
				pass
			elif op == 0x02:
				# RET
				pc_low = self.read16(self.regs[15] | 0xF0000, True)
				self.regs[15] += 2
				pc_high = self.read8(self.regs[15] | 0xF0000, True)
				self.regs[15] += 1
				self.pc = (pc_low | (pc_high << 16)) & 0xFFFFF
			elif op == 0x04:
				# POPF
				self.flags = self.read16(self.regs[15] | 0xF0000, True)
				self.regs[15] += 2
			elif op == 0x06:
				# PUSHF
				self.regs[15] -= 2
				self.write16(self.regs[15] | 0xF0000, self.flags, True)
			elif op == 0x08:
				# CLI
				self.flags &= ~F_INT
			elif op == 0x0A:
				# SEI
				self.flags |= F_INT
			elif op == 0x0C:
				# HLT
				self.halted = True
			else:
				assert False

		elif (op & 0xE0) == 0xE0:
			subop = op & 7
			if subop == 7:
				# OP3 / OP4
				op2 = self.fetch8(True)
				reg_x = op2 & 0x0F
				op2 >>= 4

				if op & 0x08:
					# OP4
					is_store = (op2 & 1) != 0
					size = (op & 0x10)
					op2 >>= 1
					if op2 == 1:
						# $Faaaa
						imm = 0xF0000 + self.fetch16(True)
					elif op2 == 2:
						# $baa00, @y
						op_yb = self.fetch8(True)
						reg_y = (op_yb>>4)
						op_yb &= 0x0F

						imm = (op_yb<<16) + (self.fetch8(True)<<8) + self.regs[reg_y]
					elif op2 == 3:
						# $baaaa, @y
						op_yb = self.fetch8(True)
						reg_y = (op_yb>>4)
						op_yb &= 0x0F

						imm = (op_yb<<16) + self.fetch16(True) + self.regs[reg_y]
					else:
						assert False

					if is_store:
						# ST
						if size:
							self.write16(imm, self.regs[reg_x])
						else:
							self.write8(imm, self.regs[reg_x] & 0xFF)
					else:
						# LD
						if size:
							val = self.read16(imm, True)
							if reg_x != 0:
								self.regs[reg_x] = val
						else:
							val = self.read8(imm, True)
							if reg_x != 0:
								self.regs[reg_x] = (self.regs[reg_x] & 0xFF00) | val

				else:
					# OP3
					if op & 0x10:
						# OP3 imm20
						new_pc = self.fetch16(True) | (reg_x<<16)
					else:
						# OP3 reg imm8
						new_pc = ((self.fetch8(True)<<12) + self.regs[reg_x]) & 0xFFFFF

					if op2 == 0x0: # JZ
						if self.flags & F_ZERO:
							self.pc = new_pc
					elif op2 == 0x1: # JNZ
						if not (self.flags & F_ZERO):
							self.pc = new_pc
					elif op2 == 0x2: # JC
						if self.flags & F_CARRY:
							self.pc = new_pc
					elif op2 == 0x3: # JNC
						if not (self.flags & F_CARRY):
							self.pc = new_pc
					elif op2 == 0x4: # JV
						if self.flags & F_OVER:
							self.pc = new_pc
					elif op2 == 0x5: # JNV
						if not (self.flags & F_OVER):
							self.pc = new_pc
					elif op2 == 0x6: # JS
						if self.flags & F_SIGN:
							self.pc = new_pc
					elif op2 == 0x7: # JNS
						if not (self.flags & F_SIGN):
							self.pc = new_pc
					elif op2 == 0x8: # JMP
						self.pc = new_pc
					elif op2 == 0x9: # JSR
						pc_low = self.pc & 0xFFFF
						pc_high = (self.pc >> 16) & 0x0F
						self.regs[15] -= 1
						self.write8(self.regs[15] | 0xF0000, pc_high)
						self.regs[15] -= 2
						self.write16(self.regs[15] | 0xF0000, pc_low)
						self.pc = new_pc
					else:
						assert False
			else:
				# OP1 reg reg / OP2
				size = (op & 0x10) != 0
				op2 = self.fetch8(True)
				reg_x = (op2 & 0x0F)
				reg_y = (op2 >> 4)

				if op & 0x08:
					self.do_op2(size, subop, reg_x, reg_y)
				else:
					self.do_op1(size, subop, reg_x, self.regs[reg_y])
		else:
			# OP1
			size = (op & 0x10) != 0
			reg_x = op & 0x0F
			op >>= 5

			if size:
				imm = self.fetch16(True)
			else:
				imm = self.fetch8(True)

			self.do_op1(size, op, reg_x, imm)

	def run_until_halt(self):
		while not self.halted:
			self.do_cycle()

memctl = StandardMemoryController()
memctl.set_rom(StringROM(8<<10, open("bios.rom", "rb").read()))
memctl.set_slot(0, RAM(4096))
memctl.set_sys_slot(DebugSysSlot())
memctl.shadow_update()
cpu = CPU(memctl)
cpu.cold_reset()
if False:
	print "filling shadow"
	for i in xrange(1<<20):
		cpu.read8(i, False)
print "running until halt"
t_start = time.time()
cpu.run_until_halt()
t_end = time.time()
print "HALT: %05X: " % (cpu.pc-1)
print " ".join("%04X" % v for v in cpu.regs)
print "time taken: %f seconds" % (t_end - t_start,)

