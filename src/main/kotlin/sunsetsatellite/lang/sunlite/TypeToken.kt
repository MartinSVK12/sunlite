package sunsetsatellite.lang.sunlite

data class TypeToken(val token: Token, val typeParameters: List<TypeToken>, val pure: Boolean)
