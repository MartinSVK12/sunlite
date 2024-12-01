package sunsetsatellite.vm.obj

class ClassInstance(val thisClass: ResolvedClass, val methods: Array<MethodRef>, val fields: Array<FieldRef<*>>){
	var superClass: ClassInstance? = null
	override fun toString(): String {
		return "ClassInstance( ${thisClass.ref.name} )"
	}
}