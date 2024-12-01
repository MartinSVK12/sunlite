package sunsetsatellite.vm.type

enum class VmTypes(val descriptor: String, val typeClass: Class<*>) {
	INT("I", IntType::class.java),
	FLOAT("F", FloatType::class.java),
	LONG("J", LongType::class.java),
	DOUBLE("D", DoubleType::class.java),
	CHAR("C", CharType::class.java),
	BOOL("Z", BoolType::class.java),
	REFERENCE("L", RefType::class.java),

	//array is actually a reference type but is here anyway for the descriptor
	ARRAY("[", RefType::class.java),
	//void can only be used in methods as the return type
	VOID("V",Nothing::class.java);

	companion object {
		fun getType(descriptor: String): VmTypes {
			return entries.first { descriptor.startsWith(it.descriptor) }
		}
	}
}