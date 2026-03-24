cmake_minimum_required(VERSION 3.10)
project("shiinaSign_native")

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

# Dobby - inline hook framework
# Add Dobby as subdirectory (source should be at jni/dobby/)
set(DOBBY_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/dobby)

if(EXISTS ${DOBBY_SOURCE_DIR}/CMakeLists.txt)
    # Dobby official CMake build
    set(DOBBY_DEBUG OFF CACHE BOOL "" FORCE)
    # Use dobby_static target (static library linked into our .so)
    add_subdirectory(${DOBBY_SOURCE_DIR} dobby)
    set(DOBBY_LIB dobby_static)
else()
    message(WARNING "Dobby source not found at ${DOBBY_SOURCE_DIR}. ECDH hook will not work.")
    add_library(dobby_static STATIC IMPORTED)
    set_target_properties(dobby_static PROPERTIES IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/dobby_stub.a")
    set(DOBBY_LIB dobby_static)
endif()

# Our native hook library
add_library(shiinaSign_native SHARED
        ecdh_hook.c)

find_library(log-lib log)

target_include_directories(shiinaSign_native PRIVATE
        ${DOBBY_SOURCE_DIR}/include
)

target_link_libraries(shiinaSign_native
        ${log-lib}
        dl
        ${DOBBY_LIB}
)
