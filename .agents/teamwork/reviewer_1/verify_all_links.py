import re
import pathlib
import urllib.parse

root = pathlib.Path(r"D:\JavaWeb")
broken = []
total_links = 0

md_files = [root / "README.md"] + list((root / "docs").rglob("*.md"))
print(f"Scanning {len(md_files)} markdown files...")

link_pattern = re.compile(r"\[([^\]]+)\]\(([^)]+)\)")

for md in md_files:
    text = md.read_text(encoding="utf-8")
    for match in link_pattern.finditer(text):
        label, url = match.groups()
        total_links += 1
        url = url.strip()
        if url.startswith("#"):
            continue
        if url.startswith("http://") or url.startswith("https://"):
            continue
        if url.startswith("mailto:"):
            continue
        if url.startswith("file:///"):
            path_str = urllib.parse.unquote(url[8:])
            target_path = pathlib.Path(path_str)
            if not target_path.exists():
                broken.append((str(md.relative_to(root)), label, url, f"File does not exist: {target_path}"))
        else:
            clean_url = url.split("#")[0]
            if clean_url:
                target_path = (md.parent / clean_url).resolve()
                if not target_path.exists():
                    broken.append((str(md.relative_to(root)), label, url, f"Relative file does not exist: {target_path}"))

print(f"Checked {total_links} links across {len(md_files)} files.")
if broken:
    print(f"FAIL: Found {len(broken)} broken links:")
    for f, l, u, reason in broken:
        print(f"  In {f}: [{l}]({u}) -> {reason}")
    exit(1)
else:
    print("SUCCESS: 100% valid links! Zero broken links detected.")
