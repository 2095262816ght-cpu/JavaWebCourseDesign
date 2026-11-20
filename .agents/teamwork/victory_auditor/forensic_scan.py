import os
import re

suspicious = []
repo_root = r"D:\JavaWeb"

for root, dirs, files in os.walk(repo_root):
    if any(p in root for p in [".git", "node_modules", "target", ".agents"]):
        continue
    for f in files:
        if f.endswith((".java", ".ts", ".vue")):
            path = os.path.join(root, f)
            with open(path, "r", encoding="utf-8", errors="ignore") as fp:
                lines = fp.readlines()
            for idx, line in enumerate(lines):
                line_str = line.strip()
                # Check for trivially true assertions
                if re.search(r"assertTrue\s*\(\s*true\s*\)", line_str):
                    suspicious.append(f"{path}:{idx+1}: {line_str}")
                if re.search(r"expect\s*\(\s*true\s*\)\s*\.toBe\s*\(\s*true\s*\)", line_str):
                    suspicious.append(f"{path}:{idx+1}: {line_str}")
                if re.search(r"assertEquals\s*\(\s*([\"\'\d\w]+)\s*,\s*\1\s*\)", line_str):
                    suspicious.append(f"{path}:{idx+1}: {line_str}")

print(f"Total dummy assertions found: {len(suspicious)}")
for s in suspicious:
    print(s)
