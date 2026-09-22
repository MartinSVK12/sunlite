//val t := tupleOf(1,"lmao",true) as Tuple<Int,String,Boolean>;
//val t := (1,"hmm",true);

//val (n, s, b) = t;

//print(n);
//print(s);
//print(b);

//val (first, second) = ["lmao", "kek"];
//print(first);
//print(second);

//val arr := [("first",1),("second",2),("third",3)];
//print(arr);

//foreach(var (s, i) in arr){
//    print(s);
//    print(i);
//}

//var first := "";
//var second := "";
//val t := ("hello","world");

//(first, second) = t;
//{first = t[0]; second = t[1];}

//print(first);
//print(second);

class A {
    var first := "";
    var second := "";

    init(t: Tuple<String,String>){
        (this.first, this.second) = t;
    }

    func show(){
        print(first);
        print(second);
    }
}

A(("hello","world")).show();