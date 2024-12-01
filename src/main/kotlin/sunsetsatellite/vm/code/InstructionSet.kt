package sunsetsatellite.vm.code

enum class InstructionSet(val clazz: Class<out Instruction>,
                          val index: Int,
                          val arguments: Int,
                          val argumentTypes: Array<Class<*>>
	) {
	NOP            (nop::class.java,            0, 0, arrayOf()),
	VRETURN        (vreturn::class.java,        0, 0, arrayOf()),
	ARETURN        (areturn::class.java,        0, 0, arrayOf()),
	GOTO           (goto::class.java,           0, 1, arrayOf(Int::class.java)),
	LDC            (ldc::class.java,            0, 1, arrayOf(Int::class.java)),
	ICONST         (iconst::class.java,         0, 1, arrayOf(Int::class.java)),
	IADD           (iadd::class.java,           0, 0, arrayOf()),
	ISUB           (isub::class.java,           0, 0, arrayOf()),
	IDIV           (idiv::class.java,           0, 0, arrayOf()),
	IMUL           (imul::class.java,           0, 0, arrayOf()),
	IREM           (irem::class.java,           0, 0, arrayOf()),
	INEG           (ineg::class.java,           0, 0, arrayOf()),
	FCONST         (fconst::class.java,         0, 1, arrayOf(Float::class.java)),
	FADD           (fadd::class.java,           0, 0, arrayOf()),
	FSUB           (fsub::class.java,           0, 0, arrayOf()),
	FDIV           (fdiv::class.java,           0, 0, arrayOf()),
	FMUL           (fmul::class.java,           0, 0, arrayOf()),
	FREM           (frem::class.java,           0, 0, arrayOf()),
	FNEG           (fneg::class.java,           0, 0, arrayOf()),
	IAND           (iand::class.java,           0, 0, arrayOf()),
	IOR            (ior::class.java,            0, 0, arrayOf()),
	IXOR           (ixor::class.java,           0, 0, arrayOf()),
	ISHL           (ishl::class.java,           0, 0, arrayOf()),
	ISHR           (ishr::class.java,           0, 0, arrayOf()),
	IUSHR          (iushr::class.java,          0, 0, arrayOf()),
	ILOAD          (iload::class.java,          0, 1, arrayOf(Int::class.java)),
	ISTORE         (istore::class.java,         0, 1, arrayOf(Int::class.java)),
	FLOAD          (fload::class.java,          0, 1, arrayOf(Int::class.java)),
	FSTORE         (fstore::class.java,         0, 1, arrayOf(Int::class.java)),
	IINC           (iinc::class.java,           0, 2, arrayOf(Int::class.java, Int::class.java)),
	I2F            (i2f::class.java,            0, 0, arrayOf()),
	F2I            (f2i::class.java,            0, 0, arrayOf()),
	IIF            (iif::class.java,            0, 1, arrayOf(Int::class.java, Condition::class.java)),
	IIFCMP         (iifcmp::class.java,         0, 2, arrayOf(Int::class.java, Condition::class.java)),
	IFNULL         (ifnull::class.java,         0, 1, arrayOf(Int::class.java)),
	IFNONNULL      (ifnonnull::class.java,      0, 1, arrayOf(Int::class.java)),
	SWAP           (swap::class.java,           0, 0, arrayOf()),
	POP            (pop::class.java,            0, 0, arrayOf()),
	NEW            (new::class.java,            0, 1, arrayOf(Int::class.java)),
	NEWARRAY       (newarray::class.java,       0, 1, arrayOf(Int::class.java)),
	ARRAYLENGTH    (arraylength::class.java,    0, 0, arrayOf()),
	IASTORE        (iastore::class.java,        0, 0, arrayOf()),
	FASTORE        (fastore::class.java,        0, 0, arrayOf()),
	CASTORE        (castore::class.java,        0, 0, arrayOf()),
	AASTORE        (aastore::class.java,        0, 0, arrayOf()),
	IALOAD         (iaload::class.java,         0, 0, arrayOf()),
	FALOAD         (faload::class.java,         0, 0, arrayOf()),
	CALOAD         (caload::class.java,         0, 0, arrayOf()),
	AALOAD         (aaload::class.java,         0, 0, arrayOf()),
	INVOKEINIT     (invokeinit::class.java,     0, 0, arrayOf()),
	INVOKESTATIC   (invokestatic::class.java,   0, 1, arrayOf(Int::class.java)),
	INVOKESUPER    (invokesuper::class.java,    0, 1, arrayOf(Int::class.java)),
	INVOKEINSTANCE (invokeinstance::class.java, 0, 1, arrayOf(Int::class.java)),
	NEWNULL        (newnull::class.java,        0, 0, arrayOf()),
	DUP            (dup::class.java,            0, 0, arrayOf()),
	PUTSTATIC      (putstatic::class.java,      0, 1, arrayOf(Int::class.java)),
	GETSTATIC      (getstatic::class.java,      0, 1, arrayOf(Int::class.java)),
	PUTFIELD       (putfield::class.java,       0, 1, arrayOf(Int::class.java)),
	GETFIELD       (getfield::class.java,       0, 1, arrayOf(Int::class.java)),
	ATHROW         (athrow::class.java,         0, 0, arrayOf()),
	ALOAD          (aload::class.java,          0, 1, arrayOf(Int::class.java)),
	ASTORE         (astore::class.java,         0, 1, arrayOf(Int::class.java)),
	INSTANCEOF     (instanceof::class.java,     0, 1, arrayOf(Int::class.java)),
	BREAKPOINT     (breakpoint::class.java,     0, 0, arrayOf()),
	DEBUG          (debug::class.java,          0, 1, arrayOf(String::class.java));


	companion object {
		// Method to find the InstructionType based on the index
		fun fromIndex(index: Int): InstructionSet? {
			return entries.find { it.index == index }
		}

		// Method to find the Instruction class based on the index
		fun findClassByIndex(index: Int): Class<out Instruction>? {
			return fromIndex(index)?.clazz
		}
	}
}