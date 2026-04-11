package sunsetsatellite.sunlite.lang

data class TypeToken(val tokens: Map<Token, List<TypeToken>>, val typeParameters: List<TypeToken>)
