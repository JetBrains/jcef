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
            "${CEF_PATCH}"
    WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}
    RESULT_VARIABLE git_apply_result
  )
  file(REMOVE ${CEF_PATCH})

  if(NOT git_apply_result STREQUAL "0")
    message(WARNING "Can't apply patches to CEF includes, result: ${git_apply_result}")
  endif()

  # erase original libcef binary
  file(REMOVE_RECURSE "third_party/cef/${CEF_DISTRIBUTION}/Release/Chromium Embedded Framework.framework")

  # extract patched binary
  execute_process(
      COMMAND "tar"
              "-xzf"
              "CEF.framework.tgz"
              "-C"
              "./third_party/cef/${CEF_DISTRIBUTION}/Release"
      WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}
      RESULT_VARIABLE tar_extract_result
  )

  if(NOT tar_extract_result STREQUAL "0")
    message(WARNING "Can't extract patched libcef, result: ${tar_extract_result}")
  endif()

endfunction()
