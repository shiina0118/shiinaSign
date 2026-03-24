cmake_minimum_required(VERSION 3.10)
project("shiinaSign_native")

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

# Dobby - inline hook framework
set(DOBBY_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/dobby)
set(DOBBY_FOUND FALSE)

if(EXISTS ${DOBBY_SOURCE_DIR}/CMakeLists.txt)
    set(DOBBY_DEBUG OFF CACHE BOOL "" FORCE)
    add_subdirectory(${DOBBY_SOURCE_DIR} dobby)
    set(DOBBY_LIB dobby_static)
    set(DOBBY_FOUND TRUE)
else()
    message(WARNING "Dobby source not found at ${DOBBY_SOURCE_DIR}. ECDH hook will not work.")
endif()

# Our native hook library
if(DOBBY_FOUND)
    add_library(shiinaSign_native SHARED ecdh_hook.c)
    target_include_directories(shiinaSign_native PRIVATE ${DOBBY_SOURCE_DIR}/include)
    target_link_libraries(shiinaSign_native ${DOBBY_LIB} dl)
else()
    # Stub build without Dobby - JNI_OnLoad will be a no-op
    add_library(shiinaSign_native SHARED ecdh_hook_stub.c)
endif()

find_library(log-lib log)
target_link_libraries(shiinaSign_native ${log-lib})
