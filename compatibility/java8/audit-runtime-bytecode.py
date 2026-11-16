from pathlib import Path
import zipfile,struct,json,sys
root=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path(__file__).resolve().parents[2]
paths=sorted(root.rglob('target/java8-runtime-classpath.txt'))
if not paths: raise SystemExit('No runtime classpath files: run Maven dependency:build-classpath first')
jars=set();owners={}
for p in paths:
 for raw in p.read_text().strip().split(':'):
  jar=Path(raw)
  if jar.suffix=='.jar':jars.add(jar);owners.setdefault(str(jar),[]).append(str(p.parent.parent.relative_to(root)))
# Runtime classpaths don't include the current module's output. Scan its packaged implementation too.
for p in paths:
 for jar in p.parent.glob('*-2.0.0-SNAPSHOT.jar'):
  if jar.name.endswith(('-tests.jar','-sources.jar','-javadoc.jar')):continue
  jars.add(jar);owners.setdefault(str(jar),[]).append(str(p.parent.parent.relative_to(root)))
results=[]
for jar in sorted(jars):
 high=[];count=0
 with zipfile.ZipFile(jar) as z:
  for n in z.namelist():
   if not n.endswith('.class') or n.startswith('META-INF/versions/') or n.endswith('module-info.class'):continue
   data=z.read(n);major=struct.unpack('>H',data[6:8])[0];count+=1
   if major>52:high.append({'class':n,'major':major})
 results.append({'jar':str(jar),'classes':count,'aboveJava8':high,'modules':owners[str(jar)]})
out={'scope':'Selected production runtime dependency classpaths plus own jars; multi-release overlays and module-info excluded; this checks bytecode, not all optional API paths.','classpathFiles':len(paths),'jarCount':len(results),'classCount':sum(r['classes'] for r in results),'higherMajorJarCount':sum(bool(r['aboveJava8']) for r in results),'jars':results}
output=Path(sys.argv[2] if len(sys.argv)>2 else root/'target/java8-runtime-bytecode.json')
output.parent.mkdir(parents=True,exist_ok=True)
output.write_text(json.dumps(out,indent=2)+'\n')
print({k:v for k,v in out.items() if k!='jars'})
for r in results:
 if r['aboveJava8']:print(r['jar'],r['aboveJava8'][:3])
sys.exit(bool(out['higherMajorJarCount']))
