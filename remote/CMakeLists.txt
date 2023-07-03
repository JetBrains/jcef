#
# Generate client-server pairs
#
set(CMAKE_VERBOSE_MAKEFILE ON)

set(Boost_NO_WARN_NEW_VERSIONS 1)
find_package(Boost REQUIRED COMPONENTS filesystem)
find_package(thrift CONFIG REQUIRED)

set(THRIFT_FILES
  cef_server.thrift
  cef_client.thrift
  shared.thrift
)

foreach(FILE ${THRIFT_FILES})
  message(STATUS "Compile ${FILE} to C++...")
  execute_process(
    COMMAND ${THRIFT_COMPILER} --gen cpp -r ${CMAKE_CURRENT_SOURCE_DIR}/${FILE}
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    RESULT_VARIABLE thrift_result
  )
  if(thrift_result AND NOT STATUS EQUAL 0)
    message(FATAL_ERROR "Failed to compile ${FILE}: ${thrift_result}")
  endif ()

  message(STATUS "Compile ${FILE} to Java...")
  execute_process(
    COMMAND ${THRIFT_COMPILER} --gen java:generated_annotations=suppress -out ${CMAKE_SOURCE_DIR}/java/com/jetbrains/cef/remote ${CMAKE_CURRENT_SOURCE_DIR}/${FILE}
    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/java
    RESULT_VARIABLE thrift_result
  )
  if(thrift_result AND NOT STATUS EQUAL 0)
    message(FATAL_ERROR "Failed to compile ${FILE}: ${thrift_result}")
  endif ()
endforeach()

set(CMAKE_CXX_STANDARD 14)
include_directories(${Boost_INCLUDE_DIRS})
add_compile_definitions(WIN32_LEAN_AND_MEAN)

set(thrift_codegen_SOURCES
    gen-cpp/Server.cpp
    gen-cpp/ClientHandlers.cpp
    gen-cpp/cef_client_types.cpp
    gen-cpp/shared_types.cpp
)
add_library(thrift_codegen ${thrift_codegen_SOURCES})
set_library_target_properties(thrift_codegen)

target_link_libraries(thrift_codegen thrift::thrift libcef_lib libcef_dll_wrapper ${CEF_STANDARD_LIBS})

set(shared_mem_helper_SOURCES
    CefServerHelper.cpp
)
add_library(shared_mem_helper SHARED ${shared_mem_helper_SOURCES})
target_include_directories(shared_mem_helper PUBLIC ${JNI_INCLUDE_DIRS})
#
# Set application
#
set(EXECUTABLE_NAME "CefServer")
set(PRODUCT_NAME "CefServer")

add_executable(${EXECUTABLE_NAME} WIN32
        CefUtils.cpp
        CefUtils.h
        CppServerWin.cpp
        browser/KeyEventProcessing.cpp
        browser/MouseEventProcessing.cpp
        ServerHandler.cpp
        Utils.cpp
        Utils.h
        log/Log.cpp
        log/Log.h
        handlers/RemoteClientHandler.cpp
        handlers/RemoteClientHandler.h
        handlers/RemoteRenderHandler.cpp
        handlers/RemoteRenderHandler.h
        handlers/RemoteLifespanHandler.cpp
        handlers/RemoteLifespanHandler.h
        handlers/RemoteLoadHandler.cpp
        handlers/RemoteLoadHandler.h
        handlers/RemoteAppHandler.cpp
        handlers/RemoteAppHandler.h
        handlers/RemoteBrowserProcessHandler.cpp
        handlers/RemoteBrowserProcessHandler.h
        handlers/RemoteDisplayHandler.cpp
        handlers/RemoteDisplayHandler.h
        RemoteObjects.h
        network/RemoteRequestHandler.cpp
        network/RemoteRequestHandler.h
        network/RemoteRequest.cpp
        network/RemoteRequest.h
        network/RemoteResourceRequestHandler.cpp
        network/RemoteResourceRequestHandler.h
        network/RemoteCookieAccessFilter.cpp
        network/RemoteCookieAccessFilter.h
        network/RemoteResourceHandler.cpp
        network/RemoteResourceHandler.h
        network/RemoteResponse.cpp
        network/RemoteResponse.h
        network/RemotePostData.cpp
        network/RemotePostData.h
        network/RemotePostDataElement.cpp
        network/RemotePostDataElement.h
        callback/RemoteCallback.cpp
        callback/RemoteCallback.h
        callback/RemoteAuthCallback.cpp
        callback/RemoteAuthCallback.h
        router/RemoteMessageRouter.cpp
        router/RemoteMessageRouter.h
        router/MessageRoutersManager.cpp
        router/MessageRoutersManager.h
        router/RemoteMessageRouterHandler.cpp
        router/RemoteMessageRouterHandler.h
        router/RemoteQueryCallback.cpp
        router/RemoteQueryCallback.h
        browser/ClientsManager.cpp
        browser/ClientsManager.h
        CppServerWin.cpp
)

target_include_directories(${EXECUTABLE_NAME} PRIVATE ${CEF_INCLUDE_PATH})
add_logical_target("libcef_lib" "${CEF_LIB_DEBUG}" "${CEF_LIB_RELEASE}")
target_link_libraries(${EXECUTABLE_NAME} libcef_lib ${CEF_STANDARD_LIBS} thrift_codegen)

target_compile_options(${EXECUTABLE_NAME} PRIVATE /MP;/Gy;/GR;/W4;/WX;/wd4100;/wd4127;/wd4244;/wd4324;/wd4481;/wd4512;/wd4701;/wd4702;/wd4996;/wd4706;/wd4005;/Zi ${CEF_CXX_COMPILER_FLAGS})
target_compile_options(${EXECUTABLE_NAME} PRIVATE $<$<CONFIG:Debug>:/MTd;/RTC1;/Od ${CEF_CXX_COMPILER_FLAGS_DEBUG}>)
target_compile_options(${EXECUTABLE_NAME} PRIVATE $<$<CONFIG:Release>:/MT;/O2;/Ob2;/GF ${CEF_CXX_COMPILER_FLAGS_RELEASE}>)

target_compile_definitions(${EXECUTABLE_NAME} PRIVATE ${CEF_COMPILER_DEFINES})
target_compile_definitions(${EXECUTABLE_NAME} PRIVATE $<$<CONFIG:Debug>:${CEF_COMPILER_DEFINES_DEBUG}>)
target_compile_definitions(${EXECUTABLE_NAME} PRIVATE $<$<CONFIG:Release>:${CEF_COMPILER_DEFINES_RELEASE}>)

target_include_directories(${EXECUTABLE_NAME} PRIVATE ${CEF_INCLUDE_PATH})

if(CEF_LINKER_FLAGS)
    string(REPLACE ";" " " _flags_str "${CEF_LINKER_FLAGS}")
    set_property(TARGET ${EXECUTABLE_NAME} PROPERTY LINK_FLAGS ${_flags_str})
endif()
if(CEF_LINKER_FLAGS_DEBUG)
    string(REPLACE ";" " " _flags_str "${CEF_LINKER_FLAGS_DEBUG}")
    set_property(TARGET ${EXECUTABLE_NAME} PROPERTY LINK_FLAGS_DEBUG ${_flags_str})
endif()
if(CEF_LINKER_FLAGS_RELEASE)
    string(REPLACE ";" " " _flags_str "${CEF_LINKER_FLAGS_RELEASE}")
    set_property(TARGET ${EXECUTABLE_NAME} PROPERTY LINK_FLAGS_RELEASE ${_flags_str})
endif()
