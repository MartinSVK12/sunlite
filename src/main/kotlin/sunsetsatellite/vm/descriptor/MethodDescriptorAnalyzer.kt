package sunsetsatellite.vm.descriptor

import sunsetsatellite.vm.method.MethodDescriptorInfo
import sunsetsatellite.vm.type.VmTypes

object MethodDescriptorAnalyzer {
	fun analyze(descriptor: String): MethodDescriptorInfo {
		if(descriptor.isEmpty()) throw IllegalStateException("empty descriptor")
		var returnType: Descriptor? = null
		val arguments: MutableList<Descriptor> = mutableListOf()

		var inArgs = false
		var outOfArgs = false
		var inRefArg = false
		var inArrayArg = false
		var refString = ""
		if (!descriptor.contains("(") || !descriptor.contains(")")) {
			throw IllegalStateException("invalid method descriptor: $descriptor")
		}
		if(descriptor.endsWith(")") || !descriptor.startsWith("(")){
			throw IllegalStateException("invalid method descriptor: $descriptor")
		}
		for ((i, c) in descriptor.withIndex()) {
			if(!inArgs && c != '(') throw IllegalStateException("invalid method descriptor: $descriptor")
			if(inArgs && c == '(') throw IllegalStateException("invalid method descriptor: $descriptor")
			if(inArgs && c == ')') {
				outOfArgs = true
				continue
			}
			if(outOfArgs){
				if(inRefArg && c == ';') {
					if(refString.trim().isEmpty()) throw IllegalStateException("invalid method descriptor: $descriptor")
					if(inArrayArg){
						returnType = Descriptor(VmTypes.ARRAY, "L$refString;")
						break
					}
					returnType = Descriptor(VmTypes.REFERENCE,refString)
					if(i+1 < descriptor.length) throw IllegalStateException("invalid method descriptor: $descriptor")
					break
				}
				if(!inRefArg){
					if(inArrayArg && c == '['){
						TODO("multidimensional arrays not yet supported")
					}
					if(inArrayArg && c == 'V') throw IllegalStateException("invalid method descriptor: $descriptor: array cant be of type void")
					val type =
						try {
							VmTypes.getType(c.toString())
						} catch (e: Exception) {
							throw IllegalStateException("invalid method descriptor type $c in $descriptor",e)
						}
					if(inArrayArg){
						if(c != 'L'){
							inArrayArg = false
							arguments.add(Descriptor(VmTypes.ARRAY, c.toString()))
							continue
						}
					}
					val desc: Descriptor =
						when(type) {
							VmTypes.INT -> Descriptor(type,"int")
							VmTypes.FLOAT -> Descriptor(type,"float")
							VmTypes.LONG -> Descriptor(type,"long")
							VmTypes.DOUBLE -> Descriptor(type,"double")
							VmTypes.CHAR -> Descriptor(type,"char")
							VmTypes.BOOL -> Descriptor(type,"bool")
							VmTypes.REFERENCE -> {
								refString = ""
								inRefArg = true
								continue
							}
							VmTypes.ARRAY -> {
								inArrayArg = true
								continue
							}
							VmTypes.VOID -> Descriptor(VmTypes.VOID,"void")
						}
					if(i+1 < descriptor.length) throw IllegalStateException("invalid method descriptor: $descriptor")
					returnType = desc
					break
				}
			}
			if(!inArgs && c == '('){
				inArgs = true
				continue
			}
			if(inRefArg){
				if(c == ';') {
					if(refString.trim().isEmpty()) throw IllegalStateException("invalid method descriptor: $descriptor")
					inRefArg = false
					if(inArrayArg){
						inArrayArg = false
						arguments.add(Descriptor(VmTypes.ARRAY, "L$refString;"))
						continue
					}
					arguments.add(Descriptor(VmTypes.REFERENCE,refString))
					refString = ""
					continue
				} else {
					refString += c
					continue
				}
			}
			if(inArrayArg && c == '['){
				TODO("multidimensional arrays not yet supported")
			}
			if(inArrayArg && c == 'V') throw IllegalStateException("invalid method descriptor: $descriptor: array cant be of type void")
			val type =
				try {
					VmTypes.getType(c.toString())
				} catch (e: Exception) {
					throw IllegalStateException("invalid method descriptor type $c in $descriptor",e)
				}
			if(inArrayArg){
				if(c != 'L'){
					inArrayArg = false
					arguments.add(Descriptor(VmTypes.ARRAY, c.toString()))
					continue
				}
			}
			val desc: Descriptor =
				when(type) {
					VmTypes.INT -> Descriptor(type,"int")
					VmTypes.FLOAT -> Descriptor(type,"float")
					VmTypes.LONG -> Descriptor(type,"long")
					VmTypes.DOUBLE -> Descriptor(type,"double")
					VmTypes.CHAR -> Descriptor(type,"char")
					VmTypes.BOOL -> Descriptor(type,"bool")
					VmTypes.REFERENCE -> {
						refString = ""
						inRefArg = true
						continue
					}
					VmTypes.ARRAY -> {
						inArrayArg = true
						continue
					}
					VmTypes.VOID -> throw IllegalStateException("invalid method descriptor: $descriptor: cant have void as an argument type")
				}
			arguments.add(desc)
		}
		if(returnType == null) throw IllegalStateException("invalid method descriptor: $descriptor")
		return MethodDescriptorInfo(returnType, arguments.toTypedArray())
	}
}

