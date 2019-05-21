# Copyright 2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

install_name_tool -change @rpath/libjvm.dylib @loader_path/server/libjvm.dylib libjcef.dylib
install_name_tool -change @rpath/libjawt.dylib @loader_path/libjawt.dylib libjcef.dylib
