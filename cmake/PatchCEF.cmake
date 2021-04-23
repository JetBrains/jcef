function(PatchCEF platform version)
  message(STATUS "Apply patches to CEF includes...")

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
endfunction()
