# Copyright (c) 2016 The Chromium Embedded Framework Authors. All rights
# reserved. Use of this source code is governed by a BSD-style license that
# can be found in the LICENSE file.

# Download the CEF binary distribution for |platform| and |version| to
# |download_dir|. The |CEF_ROOT| variable will be set in global scope pointing
# to the extracted location.
# Visit https://cef-builds.spotifycdn.com/index.html for the list of
# supported platforms and versions.

function(DownloadCEF platform version download_dir)
  # Specify the binary distribution type and download directory.
  if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(CEF_DISTRIBUTION "cef_binary_${version}_${platform}")
  else ()
    set(CEF_DISTRIBUTION "cef_binary_${version}_${platform}_minimal")
  endif ()

  set(CEF_DOWNLOAD_DIR "${download_dir}")

  # The location where we expect the extracted binary distribution.
  set(CEF_ROOT "${CEF_DOWNLOAD_DIR}/${CEF_DISTRIBUTION}" CACHE INTERNAL "CEF_ROOT")

    #cef_binary_104.4.26+g4180781+chromium-104.0.5112.102_macosarm64_minimal.tar.bz2
  # Download and/or extract the binary distribution if necessary.
  if(NOT IS_DIRECTORY "${CEF_ROOT}")
    if(NOT EXISTS "${CEF_DOWNLOAD_DIR}/${CEF_DISTRIBUTION}.zip" AND
       NOT EXISTS "${CEF_DOWNLOAD_DIR}/${CEF_DISTRIBUTION}.tar.bz2")
      set(CEF_DOWNLOAD_URL "https://cache-redirector.jetbrains.com/intellij-jbr/${CEF_DISTRIBUTION}.zip")
      string(REPLACE "+" "%2B" CEF_DOWNLOAD_URL_ESCAPED ${CEF_DOWNLOAD_URL})
      # Download the sha256sum hash for the binary distribution.
      message(STATUS "Downloading ${CEF_DISTRIBUTION}.zip.checksum from ${CEF_DOWNLOAD_URL_ESCAPED}...")
      file(DOWNLOAD "${CEF_DOWNLOAD_URL_ESCAPED}.checksum" "${CEF_DOWNLOAD_DIR}/${CEF_DISTRIBUTION}.zip.checksum")
      file (SIZE "${CEF_DOWNLOAD_DIR}/${CEF_DISTRIBUTION}.zip.checksum" CHECKSUM_FILE_SIZE)
      if(NOT "${CHECKSUM_FILE_SIZE}" STREQUAL "0")
          file(READ "${CEF_DOWNLOAD_DIR}/${CEF_DISTRIBUTION}.zip.checksum" CEF_SHA256_RAW)
          string(REGEX MATCH "[0-9a-fA-F]+" CEF_SHA256 "${CEF_SHA256_RAW}")
          # Download the binary distribution and verify the hash.
          message(STATUS "Downloading intellij ${CEF_DISTRIBUTION}.zip...")
          set(CEF_DOWNLOAD_PATH ${CEF_DOWNLOAD_DIR}/${CEF_DISTRIBUTION}.zip)
          file(
            DOWNLOAD "${CEF_DOWNLOAD_URL_ESCAPED}" "${CEF_DOWNLOAD_PATH}"
            EXPECTED_HASH SHA256=${CEF_SHA256}
            SHOW_PROGRESS
            )
      else()
          set(CEF_DOWNLOAD_URL "https://cef-builds.spotifycdn.com/${CEF_DISTRIBUTION}.tar.bz2")
          string(REPLACE "+" "%2B" CEF_DOWNLOAD_URL_ESCAPED ${CEF_DOWNLOAD_URL})
          # Download the SHA1 hash for the binary distribution.
          message(STATUS "Downloading ${CEF_DISTRIBUTION}.tar.bz2.sha1 from ${CEF_DOWNLOAD_URL_ESCAPED}...")
          file(DOWNLOAD "${CEF_DOWNLOAD_URL_ESCAPED}.sha1" "${CEF_DOWNLOAD_DIR}/${CEF_DISTRIBUTION}.tar.bz2.sha1")
          file(READ "${CEF_DOWNLOAD_DIR}/${CEF_DISTRIBUTION}.tar.bz2.sha1" CEF_SHA1)
          message(STATUS "Downloading cef-builds.spotifycdn ${CEF_DISTRIBUTION}.tar.bz2...")
          set(CEF_DOWNLOAD_PATH ${CEF_DOWNLOAD_DIR}/${CEF_DISTRIBUTION}.tar.bz2)
          file(
            DOWNLOAD "${CEF_DOWNLOAD_URL_ESCAPED}" "${CEF_DOWNLOAD_PATH}"
            EXPECTED_HASH SHA1=${CEF_SHA1}
            SHOW_PROGRESS
            )
      endif()
    else()
      message(STATUS "Extracting downloaded artifact ${CEF_DOWNLOAD_PATH}...")
    endif()

    # Extract the binary distribution.
    message(STATUS "Extracting ${CEF_DOWNLOAD_PATH}...")
    execute_process(
      COMMAND ${CMAKE_COMMAND} -E tar xzf "${CEF_DOWNLOAD_PATH}"
      WORKING_DIRECTORY ${CEF_DOWNLOAD_DIR}
      )
    if("${CMAKE_SYSTEM_NAME}" STREQUAL "Linux")
      message(STATUS "Strip debugging symbols...")
      execute_process(
        COMMAND "strip" "-s" "${CEF_DOWNLOAD_DIR}/${CEF_DISTRIBUTION}/Release/libcef.so"
        WORKING_DIRECTORY ${CEF_DOWNLOAD_DIR}
      )
    endif()
  endif()
endfunction()
