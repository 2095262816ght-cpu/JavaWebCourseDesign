import os
import re
import sys
import urllib.parse
from pathlib import Path

repo_root = Path(r"D:\JavaWeb")

scanned_files = 0
total_links = 0
broken_links = []
valid_links = 0

md_files = []
for root, dirs, files in os.walk(repo_root):
    # Skip agent dirs, node_modules, target, .git
    if any(p in root for p in [".git", "node_modules", "target", ".agents"]):
        continue
    for f in files:
        if f.endswith(".md"):
            md_files.append(Path(root) / f)

print(f"Discovered {len(md_files)} markdown files to scan.")

link_pattern = re.compile(r"\[([^\]]+)\]\(([^)]+)\)")

for md_file in md_files:
    scanned_files += 1
    content = md_file.read_text(encoding="utf-8", errors="ignore")
    # remove code blocks to avoid matching examples in code
    content_no_code = re.sub(r"```[\s\S]*?```", "", content)
    matches = link_pattern.findall(content_no_code)
    for text, target in matches:
        target = target.strip()
        total_links += 1
        
        # External web links
        if target.startswith(("http://", "https://", "mailto:")):
            valid_links += 1
            continue
            
        # Pure in-page anchor links like #section
        if target.startswith("#"):
            # Check if anchor exists in this file (rough check or valid)
            valid_links += 1
            continue
            
        # Handle file:/// or relative/absolute local paths
        url_part = target.split("#")[0]
        if not url_part:
            valid_links += 1
            continue

        resolved_path = None
        if url_part.startswith("file:"):
            # e.g. file:///D:/JavaWeb/docs/...
            clean_url = url_part.replace("file:///", "").replace("file://", "")
            resolved_path = Path(clean_url)
        elif os.path.isabs(url_part):
            resolved_path = Path(url_part)
        else:
            resolved_path = (md_file.parent / url_part).resolve()

        if resolved_path and resolved_path.exists():
            valid_links += 1
        else:
            broken_links.append((str(md_file.relative_to(repo_root)), text, target, str(resolved_path)))

print(f"Scanned files: {scanned_files}")
print(f"Total markdown links checked: {total_links}")
print(f"Valid links: {valid_links}")
print(f"Broken links: {len(broken_links)}")
for source, text, target, res in broken_links:
    print(f"  FAILED: in {source}: [{text}]({target}) -> resolved to '{res}' (NOT FOUND)")

if broken_links:
    sys.exit(1)

