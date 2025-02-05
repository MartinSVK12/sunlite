package sunsetsatellite.lang.lox.natives

import sunsetsatellite.lang.lox.*

object NativeList {

    fun registerNatives(): Array<Pair<String, NativeFunction<out Any?>>> {
        return arrayOf(
            "NativeList::init" to NativeFunction { interpreter: Interpreter, arguments: List<Any?>?, thisClass: LoxClassInstance? ->

                thisClass?.let {
                    thisClass.fields["<native field>"] = LoxField(Type.NIL, FieldModifier.NORMAL, mutableListOf<Any?>())
                }

                return@NativeFunction thisClass
            },

            "NativeList::size" to NativeFunction { interpreter: Interpreter, arguments: List<Any?>?, thisClass: LoxClassInstance? ->
                thisClass?.let {
                    return@NativeFunction (thisClass.fields["<native field>"]?.value as MutableList<*>).size.toDouble()
                }
            },

            "NativeList::isEmpty" to NativeFunction { interpreter: Interpreter, arguments: List<Any?>?, thisClass: LoxClassInstance? ->
                thisClass?.let {
                    return@NativeFunction (thisClass.fields["<native field>"]?.value as MutableList<*>).isEmpty()
                }
            },

            "NativeList::clear" to NativeFunction { interpreter: Interpreter, arguments: List<Any?>?, thisClass: LoxClassInstance? ->
                thisClass?.let {
                    (thisClass.fields["<native field>"]?.value as MutableList<*>).clear()
                    return@NativeFunction null
                }
            },

            "NativeList::add" to NativeFunction { interpreter: Interpreter, arguments: List<Any?>?, thisClass: LoxClassInstance? ->
                thisClass?.let {
                    arguments?.get(0)?.let { return@NativeFunction (thisClass.fields["<native field>"]?.value as MutableList<Any?>).add(it) }
                }
            },

            "NativeList::remove" to NativeFunction { interpreter: Interpreter, arguments: List<Any?>?, thisClass: LoxClassInstance? ->
                thisClass?.let {
                    arguments?.get(0)?.let { return@NativeFunction (thisClass.fields["<native field>"]?.value as MutableList<Any?>).remove(it) }
                }
            },

            "NativeList::get" to NativeFunction { interpreter: Interpreter, arguments: List<Any?>?, thisClass: LoxClassInstance? ->
                thisClass?.let {
                    arguments?.get(0)?.let { return@NativeFunction (thisClass.fields["<native field>"]?.value as MutableList<Any?>).get((it as Double).toInt()) }
                }
            }
        )
    }
}