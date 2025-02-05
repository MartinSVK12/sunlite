package sunsetsatellite.lang.lox

data class LoxField(val type: Type, val modifier: FieldModifier, val defaultValue: Any?, var value: Any? = defaultValue) {

    fun copy(): LoxField {
        return LoxField(type, modifier, value, defaultValue)
    }

}
