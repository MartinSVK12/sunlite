func fact(n: Double): Double {
    return factIter(1.0,n);
}

@TailRec
func factIter(p: Double, n: Double): Double {
    if(n <= 0){
        return p;
    }
    return factIter(p*n,n-1);
}

var before: Double = clock();
print(fact(170.0));
var after: Double = clock();
print(after - before);