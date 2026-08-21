enum Test {
    ONE(1),
    TWO(2),
    THREE(3);

    var value: Int;

    init(v: Int) {
        value = v;
    }
}

print(Test.fromName("ONE").name);

foreach(var entry: Enum in Test.entries()){
    print(entry.name);
}