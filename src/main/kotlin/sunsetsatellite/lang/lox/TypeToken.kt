package sunsetsatellite.lang.lox

data class TypeToken(val token: Token, val typeParameters: List<TypeToken>, val pure: Boolean)
