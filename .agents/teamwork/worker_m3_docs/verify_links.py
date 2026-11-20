import os
import re
import pathlib
import urllib.parse

docs_root = pathlib.Path(r"D:\JavaWeb\docs")
broken = []
total_links = 0

md_files = list(docs_root.rglob("*.md"))
print(f"Scanning {len(md_files)} markdown files in {docs_root}...")

link_pattern = re.compile(r"\[([^\]]+)\]\(([^)]+)\)")

for md in md_files:
    text = md.read_text(encoding="utf-8")
    for match in link_pattern.finditer(text):
        label, url = match.groups()
        total_links += 1
        url = url.strip()
        if url.startswith("#"):
            continue # internal anchor
        if url.startswith("http://") or url.startswith("https://"):
            continue # external url
        if url.startswith("mailto:"):
            continue
        if url.startswith("file:///"):
            # absolute file link
            path_str = urllib.parse.unquote(url[8:]) # remove file:///
            target_path = pathlib.Path(path_str)
            if not target_path.exists():
                broken.append((str(md.relative_to(docs_root)), label, url, f"File does not exist: {target_path}"))
        else:
            # relative link
            clean_url = url.split("#")[0]
            if clean_url:
                target_path = (md.parent / clean_url).resolve()
                if not target_path.exists():
                    broken.append((str(md.relative_to(docs_root)), label, url, f"Relative file does not exist: {target_path}"))

print(f"Checked {total_links} links.")
if broken:
    print(f"FAIL: Found {len(broken)} broken links:")
    for f, l, u, reason in broken:
        print(f"  In {f}: [{l}]({u}) -> {reason}")
    exit(1)
else:
    print("SUCCESS: Zero broken links found across all docs markdown files!")
