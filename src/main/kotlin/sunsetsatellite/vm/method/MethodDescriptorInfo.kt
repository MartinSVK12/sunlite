package sunsetsatellite.vm.method

import sunsetsatellite.vm.descriptor.Descriptor

data class MethodDescriptorInfo(val returnType: Descriptor, val arguments: Array<Descriptor>) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is MethodDescriptorInfo) return false

		if (returnType != other.returnType) return false
		if (!arguments.contentEquals(other.arguments)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = returnType.hashCode()
		result = 31 * result + arguments.contentHashCode()
		return result
	}

	override fun toString(): String {
		return "MethodDescriptorInfo( ${arguments.contentToString()} -> $returnType )"
	}
}