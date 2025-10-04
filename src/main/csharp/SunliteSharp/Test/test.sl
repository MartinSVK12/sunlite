fun fib(n: Number): Number {
  if (n < 2) return n;
  return fib(n - 1) + fib(n - 2);
}

var before: Number = clock();
print(fib(35));
var after: Number = clock();
print(after - before);