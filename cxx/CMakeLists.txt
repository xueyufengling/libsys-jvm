cmake_minimum_required (VERSION 3.10)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED OFF)

project(jvmsys)

find_package(ppmp REQUIRED)
find_package(tplmp REQUIRED)
find_package(llvmmci REQUIRED)
find_package(arch REQUIRED)
find_package(sys REQUIRED)

set(PROJECT_SOURCE_INCLUDE_DIRS "${CMAKE_CURRENT_SOURCE_DIR}/include")

set(SOURCE_DIR "${CMAKE_SOURCE_DIR}/src")
file(GLOB_RECURSE SOURCES "${SOURCE_DIR}/*.cpp")

add_compile_options(-g -O0)

add_library(${PROJECT_NAME} STATIC ${SOURCES})

target_include_directories(${PROJECT_NAME} PUBLIC "${PROJECT_SOURCE_INCLUDE_DIRS}")
target_include_directories(${PROJECT_NAME} PUBLIC "${ppmp_INCLUDE_DIRS}")
target_include_directories(${PROJECT_NAME} PUBLIC "${tplmp_INCLUDE_DIRS}")
target_include_directories(${PROJECT_NAME} PUBLIC "${llvmmci_INCLUDE_DIRS}")
target_include_directories(${PROJECT_NAME} PUBLIC "${arch_INCLUDE_DIRS}")
target_include_directories(${PROJECT_NAME} PUBLIC "${sys_INCLUDE_DIRS}")

target_link_libraries(${PROJECT_NAME} PUBLIC ppmp tplmp llvmmci arch sys)

# Install
 
set(PROJECT_INSTALL_INCLUDE_DIRS "${CMAKE_INSTALL_PREFIX}/include")
set(PROJECT_INSTALL_LIBRARIES)

install(TARGETS ${PROJECT_NAME} DESTINATION "lib")
install(DIRECTORY "${PROJECT_SOURCE_INCLUDE_DIRS}/" DESTINATION "include")
message(STATUS "set copy path from \"${PROJECT_SOURCE_INCLUDE_DIRS}\" to \"${PROJECT_INSTALL_INCLUDE_DIRS}\"")

configure_file("${CMAKE_CURRENT_SOURCE_DIR}/cmake/config.cmake.in" "${CMAKE_INSTALL_PREFIX}/${PROJECT_NAME}Config.cmake" @ONLY)
message(STATUS "generated ${PROJECT_NAME}Config.cmake in: \"${CMAKE_INSTALL_PREFIX}/${PROJECT_NAME}Config.cmake\"")

message(STATUS "configure complete, run cmake --build . --target install to copy include directory")