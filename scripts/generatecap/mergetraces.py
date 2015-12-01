import subprocess,glob,sys,time

if (len(sys.argv)<2):
  print('usage: fileprefix outputfile');
  exit()

prefix=sys.argv[1];
output=sys.argv[2]

files = glob.glob(prefix+'*');
files.sort(key=lambda f: int(f.split('_')[1]))
cmd = 'mergecap -w '+output+' ';
for f in files:
  cmd=cmd+f+' ';

subprocess.call(cmd,shell=True)

