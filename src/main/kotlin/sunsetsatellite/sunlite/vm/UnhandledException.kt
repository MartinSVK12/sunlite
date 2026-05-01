package sunsetsatellite.sunlite.vm

class UnhandledException(val e: SLClassInstanceObj) : Exception(e.value.fields["message"]?.value.toString())