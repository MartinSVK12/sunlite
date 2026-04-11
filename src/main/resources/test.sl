import "/stdlib/io.sl";

func main(){
    val file: File = IO.open("sl_file_open");
    print(file);
}

main();

