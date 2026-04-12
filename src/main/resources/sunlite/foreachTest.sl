//import "/array.sl";
import "/array.sl";

val arr: Array<Int> = arrayOf(0,1,2,3,4,5) as Array<Int>;

//for(var iter = arr.getIterator(); iter.hasNext() == true; iter.next()){
//    val e: Any? = iter.current() as Any?;
//    print(e);
//}

foreach(var element in arr){
    print(typeOf(element));
}
//print(arr.getIterator());