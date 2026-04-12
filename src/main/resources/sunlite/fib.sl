func fib(n: Double): Double {
  if (n < 2) return n;
  return fib(n - 1) + fib(n - 2);
}

print("running fib test...");

var before: Double = clock();
print(fib(35));
var after: Double = clock();
print(after - before);