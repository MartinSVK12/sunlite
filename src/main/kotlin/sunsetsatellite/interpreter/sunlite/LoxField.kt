package sunsetsatellite.interpreter.sunlite

import sunsetsatellite.lang.sunlite.FieldModifier
import sunsetsatellite.lang.sunlite.Type

data class LoxField(val type: Type, val modifier: FieldModifier, val defaultValue: Any?, var value: Any? = defaultValue) {

    fun copy(): LoxField {
        return LoxField(type, modifier, value, defaultValue)
    }

}
