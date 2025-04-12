//
// Created by sunsetsatellite on 12. 2. 2025.
//

#ifndef clox_compiler_h
#define clox_compiler_h
#include "chunk.h"
#include "object.h"

ObjFunction* compile(const char *source);

#endif //clox_compiler_h