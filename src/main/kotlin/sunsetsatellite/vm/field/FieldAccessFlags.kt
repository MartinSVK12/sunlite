package sunsetsatellite.vm.field

class FieldAccessFlags {
	var PUBLIC: Boolean = false
	var PROTECTED: Boolean = false
	var PRIVATE: Boolean = false
	var STATIC: Boolean = false
	var FINAL: Boolean = false
	var SYNTHETIC: Boolean = false
	override fun toString(): String {
		return "FieldAccessFlags( ${if(PUBLIC) "PUBLIC " else ""}${if(PROTECTED) "PROTECTED " else ""}${if(PRIVATE) "PRIVATE " else ""}${if(STATIC) "STATIC " else ""}${if(FINAL) "FINAL " else ""}${if(SYNTHETIC) "SYNTHETIC " else ""})"
	}


}