import subprocess
import os
import sys
import signal
import time

def is_number(s):
    try:
        int(s)
        return True
    except ValueError:
        return False

def kill_gpu_process(except_list):
	result = subprocess.run(["pgrep", "-f", "type=gpu-process"], stdout=subprocess.PIPE)
	pids = result.stdout.decode('utf-8')
	for pid in pids.split('\n'):
		if (pid in except_list):
			print("skip " + pid)
			continue
		if is_number(pid):
			print("kill " + pid)
			os.kill(int(pid), signal.SIGTERM) #or signal.SIGKILL 


duration = 60 	# sec
pause = 2 		# sec
start = time.time()
while time.time() - start < duration:
	kill_gpu_process(sys.argv)
	time.sleep(pause)
