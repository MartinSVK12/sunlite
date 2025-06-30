fun arrayForEach(arr: Array, f: Function<Any,Nil>){
    for(var i: Number = 0;i < sizeOf(arr);i = i + 1){
        f(arr[i]);
    }
}

arrayForEach(arrayOf("lmao",69,true,nil), fun(o) { print(o); });