package sunsetsatellite.vm.descriptor

import sunsetsatellite.vm.type.VmTypes

object FieldDescriptorAnalyzer {
	fun analyze(descriptor: String): Descriptor {
		if(descriptor.isEmpty()) throw IllegalStateException("empty descriptor")
		if (descriptor.contains("(") || descriptor.contains(")")) throw IllegalStateException("invalid field descriptor: $descriptor")
		var refString: String

		val type =
			try {
				VmTypes.getType(descriptor.codePointAt(0).toChar().toString())
			} catch (e: Exception) {
				throw IllegalStateException("invalid field descriptor: $descriptor",e)
			}

		val desc: Descriptor =
			when(type) {
				VmTypes.INT -> Descriptor(type, "int")
				VmTypes.FLOAT -> Descriptor(type, "float")
				VmTypes.LONG -> Descriptor(type, "long")
				VmTypes.DOUBLE -> Descriptor(type, "double")
				VmTypes.CHAR -> Descriptor(type, "char")
				VmTypes.BOOL -> Descriptor(type, "bool")
				VmTypes.REFERENCE -> {
					refString = ""
					var desc: Descriptor? = null
					if(!descriptor.endsWith(";")) throw IllegalStateException()
					else {
						for(c in descriptor.substring(1)){
							if(c == ';') {
								if(refString.trim().isEmpty()) throw IllegalStateException("invalid field descriptor: $descriptor")
								desc = Descriptor(VmTypes.REFERENCE, refString)
								break
							} else {
								refString += c
								continue
							}
						}
					}
					if(desc == null) throw IllegalStateException("invalid field descriptor: $descriptor")
					return desc
				}
				VmTypes.ARRAY -> {
					val arrayType =
						try {
							VmTypes.getType(descriptor.codePointAt(1).toChar().toString())
						} catch (e: Exception) {
							throw IllegalStateException("invalid field descriptor: $descriptor",e)
						}
					when(arrayType) {
						VmTypes.INT -> Descriptor(VmTypes.ARRAY, "I")
						VmTypes.FLOAT -> Descriptor(VmTypes.ARRAY, "F")
						VmTypes.LONG -> Descriptor(VmTypes.ARRAY, "J")
						VmTypes.DOUBLE -> Descriptor(VmTypes.ARRAY, "D")
						VmTypes.CHAR -> Descriptor(VmTypes.ARRAY, "C")
						VmTypes.BOOL -> Descriptor(VmTypes.ARRAY, "Z")
						VmTypes.REFERENCE -> {
							refString = ""
							var desc: Descriptor? = null
							if(!descriptor.endsWith(";")) throw IllegalStateException()
							else {
								for(c in descriptor.substring(1)){
									if(c == ';') {
										if(refString.trim().isEmpty()) throw IllegalStateException("invalid field descriptor: $descriptor")
										desc = Descriptor(VmTypes.ARRAY, "$refString;")
										break
									} else {
										refString += c
										continue
									}
								}
							}
							if(desc == null) throw IllegalStateException("invalid field descriptor: $descriptor")
							return desc
						}
						VmTypes.ARRAY -> TODO("multidimensional arrays not yet supported")
						VmTypes.VOID -> throw IllegalStateException("invalid field descriptor: $descriptor: cant have void as array type")
					}
				}
				VmTypes.VOID -> throw IllegalStateException("invalid field descriptor: $descriptor: cant have void as field type")
			}
		return desc
	}
}