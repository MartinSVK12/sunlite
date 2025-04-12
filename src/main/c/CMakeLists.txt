cmake_minimum_required(VERSION 3.30)
project(clox C)

set(CMAKE_C_STANDARD 99)

add_executable(
   clox main.c
        common.h
        chunk.h
        chunk.c
        memory.h
        memory.c
        debug.c
        debug.h
        value.c
        value.h
        vm.c
        vm.h
        compiler.c
        compiler.h
        scanner.c
        scanner.h
        object.c
        object.h
        table.c
        table.h
)