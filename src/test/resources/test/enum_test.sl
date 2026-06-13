enum Test {
    ONE(1),
    TWO(2),
    THREE(3);

    var value: Int;

    init(v: Int) {
        value = v;
    }
}

print(Enum.fromName(Test, "ONE").name);