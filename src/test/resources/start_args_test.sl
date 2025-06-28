fun arrayForEach(arr: array, f: function<any,nil>){
    for(var i: number = 0;i < sizeOf(arr);i = i + 1){
        f(arr[i]);
    }
}

arrayForEach(launchArgs(), fun(o) { print(o); });