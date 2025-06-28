class<T> A {

    fun b(t: generic<T>){
        print(t);
    }

}

A(<string>).b("yay!");