package sunsetsatellite.lang.sunlite

data class TypeToken(val tokens: Map<Token, List<TypeToken>>, val typeParameters: List<TypeToken>)
