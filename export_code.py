import os

skip_exts = ['.class',".jar",".woff2"]  # Add extensions you want to skip
root = os.path.abspath(".")

for dirpath, _, filenames in os.walk(root):
    for f in filenames:
        if any(f.lower().endswith(ext) for ext in skip_exts):
            continue
        print(f)
        filepath = os.path.join(dirpath, f)
        relpath = os.path.relpath(filepath, root)
        try:
            with open(filepath, 'r', encoding='utf-8') as file:
                content = file.read()
                print(f"--- {relpath} ---\n{content}\n")
        except Exception as e:
            print(f"Could not read {relpath}: {e}")
