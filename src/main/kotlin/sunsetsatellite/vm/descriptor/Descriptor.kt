package sunsetsatellite.vm.descriptor

import sunsetsatellite.vm.type.VmTypes

data class Descriptor(val type: VmTypes, val ref: String){
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Descriptor) return false

		if (type != other.type) return false
		if (ref != other.ref) return false

		return true
	}

	override fun hashCode(): Int {
		var result = type.hashCode()
		result = 31 * result + ref.hashCode()
		return result
	}

	override fun toString(): String {
		return "( $type | $ref )"
	}
}