import ModuleLoader from "/sunlite/stdlib/loader";
import BaseModuleLoader from "/sunlite/stdlib/loader";

val loader: BaseModuleLoader = BaseModuleLoader();
setModuleLoader(loader);

import ArrayList from "/sunlite/stdlib/list";
val list := ArrayList(<Int>);
print(list);