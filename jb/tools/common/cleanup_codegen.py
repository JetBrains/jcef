import re
import os
from os.path import join, isfile

path = os.path.abspath(r"java/com/jetbrains/cef/remote/thrift_codegen")
for filename in os.listdir(path):
    fpath = join(path, filename)
    if isfile(fpath):
        with open(fpath, "rt") as fin:
            with open(fpath + ".fxd", "wt") as fout:
                for line in fin:
                    # TODO: replace with CefLog.Error
                    replaced_line = re.sub(r'org.slf4j.', r'org.apache.thrift.', line, flags = re.M)
                    fout.write(replaced_line)

        os.remove(fpath)
        os.rename(fpath + ".fxd", fpath)
