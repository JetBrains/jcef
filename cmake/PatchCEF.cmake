function(PatchCEF platform version)
  message(STATUS "Apply patches to CEF includes and binary...")

  set(CEF_DISTRIBUTION "cef_binary_${version}_${platform}")

  set(CEF_PATCH_DIR "${PROJECT_SOURCE_DIR}/jb/tools/patches")
  set(CEF_PATCH_NAME "IDEA_260275_media_access_handler")
  set(CEF_PATCH_IN "${CEF_PATCH_DIR}/${CEF_PATCH_NAME}.in")
  set(CEF_PATCH "${CEF_PATCH_DIR}/${CEF_PATCH_NAME}.patch")

  execute_process(
    COMMAND "sed"
            "s/CEF_BINARY_ROOT/${CEF_DISTRIBUTION}/g;w ${CEF_PATCH}"
            "${CEF_PATCH_IN}"
    WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}
    RESULT_VARIABLE git_patch
    OUTPUT_QUIET
  )

  execute_process(
    COMMAND "git"
            "apply"
            "-v"
            "--ignore-whitespace"
            "${CEF_PATCH}"
    WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}
    RESULT_VARIABLE git_apply_result
  )
  file(REMOVE ${CEF_PATCH})

  if(NOT git_apply_result STREQUAL "0")
    message(WARNING "Can't apply patches to CEF includes, result: ${git_apply_result}")
  endif()

  # replace original libcef binary (and dependent) with patched binaries
  if("${CMAKE_SYSTEM_NAME}" STREQUAL "Darwin")
    execute_process(
        COMMAND "rm"
                "-rf"
                "./third_party/cef/${CEF_DISTRIBUTION}/Release/Chromium Embedded Framework.framework/*"
        WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}
        RESULT_VARIABLE remove_result
      )
    if(NOT remove_result STREQUAL "0")
      message(WARNING "Can't erase origin libcef, result: ${remove_result}")
    endif()

    execute_process(
      COMMAND "cp"
              "-rfv"
              "jb_cef_binary/."
              "./third_party/cef/${CEF_DISTRIBUTION}/Release"
      WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}
      RESULT_VARIABLE replace_result
    )
  elseif("${CMAKE_SYSTEM_NAME}" STREQUAL "Linux")
    file(REMOVE_RECURSE "third_party/cef/${CEF_DISTRIBUTION}/Release/libcef.so")
    file(REMOVE_RECURSE "third_party/cef/${CEF_DISTRIBUTION}/Release/snapshot_blob.bin")
    file(REMOVE_RECURSE "third_party/cef/${CEF_DISTRIBUTION}/Release/v8_context_snapshot.bin")
    execute_process(
      COMMAND "cp"
              "-rfv"
              "jb_cef_binary/."
              "./third_party/cef/${CEF_DISTRIBUTION}/Release"
      WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}
      RESULT_VARIABLE replace_result
    )
  elseif("${CMAKE_SYSTEM_NAME}" STREQUAL "Windows")
    file(REMOVE_RECURSE "third_party/cef/${CEF_DISTRIBUTION}/Release/libcef.dll")
    file(REMOVE_RECURSE "third_party/cef/${CEF_DISTRIBUTION}/Release/snapshot_blob.bin")
    file(REMOVE_RECURSE "third_party/cef/${CEF_DISTRIBUTION}/Release/v8_context_snapshot.bin")

    execute_process(
      COMMAND "xcopy"
              "jb_cef_binary"
              ".\\third_party\\cef\\${CEF_DISTRIBUTION}\\Release"
              "/E"
      WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}
      RESULT_VARIABLE replace_result
    )
  endif()

  if(NOT replace_result STREQUAL "0")
    message(WARNING "Can't replace patched libcef, result: ${replace_result}")
  endif()
endfunction()
