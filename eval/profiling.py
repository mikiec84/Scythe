import subprocess32
import datetime
import sys
import time
import os

logging = True
timeout = 600
#synthesis_algorithm = "CanonicalEnumeratorOnTheFly"
synthesis_algorithm = "StagedEnumerator"

scythe = os.path.join("..", "out", "artifacts", "Scythe_jar", "Scythe.jar")

def run_scythe_with_aggr(output_stream, timeout):
	try:
		args = ['java', '-jar', scythe, f, synthesis_algorithm, '-aggr']
		subprocess32.call(args, stdout=output_stream, timeout=timeout)
	except:
		print "  [FAIL] timeout"

if __name__ == "__main__":

	data_dir = os.path.join("..", "data")

	benchmark_dir_list = ["recent_posts"] #"dev_set", "top_rated_posts", "sqlsynthesizer"
	log_dir = os.path.join("log", synthesis_algorithm[0]+"log_" + datetime.datetime.fromtimestamp(time.time()).strftime('%Y%m%d_%H%M'))

	if logging and not os.path.exists(log_dir):
		os.makedirs(log_dir)

	for benchmark_dir_suffix in benchmark_dir_list:

		benchmark_dir = os.path.join(data_dir, benchmark_dir_suffix)

		if not os.path.exists(benchmark_dir):
			print "[[ERROR]] benchmark directory not exists: " + benchmark_dir

		files = [os.path.join(benchmark_dir, name) for name in os.listdir(benchmark_dir)
																if os.path.isfile(os.path.join(benchmark_dir, name))]

		for f in files:

			if f.endswith("X") or ("R" in f):
				continue

			print "[[Running]] " + f
			if logging:
				log_file = os.path.join(log_dir, benchmark_dir_suffix + "__" + os.path.basename(f) + ".log")
				run_scythe_with_aggr(open(log_file, "w"), timeout)
			else:
				run_scythe_with_aggr(open(os.devnull, 'w'), timeout)
