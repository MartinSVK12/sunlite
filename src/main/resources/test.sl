class Hello {
    func greet(name: String): String {
        return "Hello, ${name}!";
    }
}

val greeting: String = Hello().greet("world");

print(greeting);