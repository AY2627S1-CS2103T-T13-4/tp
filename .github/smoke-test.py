#!/usr/bin/env python3
"""Launch a release JAR with a UI-driving Java agent in disposable directories.

Requires JDK 25 and a graphical desktop. No test classes replace production classes.
The process uses the JAR's own entry point, native libraries, UI, commands, and storage.
"""

import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile


root = Path(__file__).resolve().parent.parent
java_bin = Path(os.environ["JAVA_HOME"]) / "bin" if "JAVA_HOME" in os.environ else None


def jdk_tool(name):
    return str(java_bin / name) if java_bin else name


settings = subprocess.run([jdk_tool("java"), "-XshowSettings:properties", "-version"],
                          capture_output=True, text=True, check=True).stderr
print("\n".join(line for line in settings.splitlines()
                if any(key in line for key in ["java.runtime.version", "os.arch", "os.name", "os.version"])), flush=True)
modules = subprocess.check_output([jdk_tool("java"), "--list-modules"], text=True)
assert "javafx." not in modules, "Use a plain JDK so bundled JavaFX cannot hide a broken release package"
arm_mac = sys.platform == "darwin" and "os.arch = aarch64" in settings
default_jar = "TutorLink-mac-aarch64.jar" if arm_mac else "TutorLink.jar"
artifact = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root / "build/libs" / default_jar
output = root / "build" / "packaged-smoke"
output.mkdir(parents=True, exist_ok=True)
work = Path(tempfile.mkdtemp(prefix="run-", dir=output))
classes = work / "agent-classes"
classes.mkdir()
subprocess.run([jdk_tool("javac"), "-cp", str(artifact), "-d", str(classes),
                str(root / ".github/PackagedSmoke.java")], check=True)
manifest = work / "agent.mf"
manifest.write_text("Premain-Class: PackagedSmoke\n\n")
agent = work / "smoke-agent.jar"
subprocess.run([jdk_tool("jar"), "--create", "--file", str(agent), "--manifest", str(manifest),
                "-C", str(classes), "."], check=True)


def launch(folder, phase):
    folder.mkdir(exist_ok=True)
    jar = folder / artifact.name
    if not jar.exists():
        shutil.copy2(artifact, jar)
    marker = folder / "smoke-passed.txt"
    if marker.exists():
        marker.unlink()
    log = folder / (phase + ".log")
    with log.open("w") as stream:
        result = subprocess.run([jdk_tool("java"), "-ea", "-javaagent:" + str(agent) + "=" + phase,
                                 "-jar", jar.name], cwd=folder, stdout=stream,
                                stderr=subprocess.STDOUT, timeout=60)
    log_text = log.read_text()
    print(log_text if result.returncode else "\n".join(line for line in log_text.splitlines()
                                                     if line.startswith("PASS ")), flush=True)
    assert result.returncode == 0, (phase, result.returncode, log)
    assert marker.read_text() == phase, (phase, "completion marker missing", log)


workflow = work / "workflow"
launch(workflow, "workflow")
saved = (workflow / "data/tutorlink.json").read_bytes()
students = json.loads(saved)["students"]
assert len(students) == 1 and students[0]["name"] == "Alice Tan", students
assert students[0]["matricNumber"] == "A1234567Z", students
assert students[0]["tutorialGroup"] == "L03", students
launch(workflow, "restart")
assert (workflow / "data/tutorlink.json").read_bytes() == saved, "restart changed saved data"

for name, invalid in [("corrupt", b"{not valid JSON"),
                      ("missing-required", b'{"students":[{"name":"Protected Student","tags":[]}]}')]:
    folder = work / name
    (folder / "data").mkdir(parents=True)
    data_file = folder / "data/tutorlink.json"
    data_file.write_bytes(invalid)
    launch(folder, "invalid")
    assert data_file.read_bytes() == invalid, "invalid input file was overwritten"

print("PACKAGED SMOKE PASSED:", artifact.name, "Evidence:", work, flush=True)
