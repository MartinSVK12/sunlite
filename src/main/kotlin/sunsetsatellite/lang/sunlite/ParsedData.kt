package sunsetsatellite.lang.sunlite

data class ParsedData(val tokens: List<Token>, val statements: List<Stmt>, val collector: TypeCollector)