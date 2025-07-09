import os
import json
import re
import argparse

# === CONFIGURATION ===
FILTER_REGEX = re.compile(r'.*')
ITEMS_DIR = os.path.join("assets", "modelengine", "items")
COMBINED_FILENAME = "megcombined.json"

# === UTILS ===
def is_valid_model_file(filename):
    return filename.endswith(".json") and filename != COMBINED_FILENAME

def generate_combined_model(folder_path, folder_name, dry_run=False):
    model_entries = []

    for filename in os.listdir(folder_path):
        if not is_valid_model_file(filename):
            continue
        model_path = f"modelengine:{folder_name}/{filename[:-5]}"  # Remove .json
        model_entries.append({
            "type": "model",
            "model": model_path
        })

    combined_model = {
        "model": {
            "type": "composite",
            "models": model_entries
        }
    }

    output_path = os.path.join(folder_path, COMBINED_FILENAME)

    if dry_run:
        print(f"[DRY-RUN] Would write {COMBINED_FILENAME} in: {folder_path}")
        print(json.dumps(combined_model, indent=4))
        print()
    else:
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(combined_model, f, indent=4)
        print(f"[✓] Written: {output_path}")

def main():
    parser = argparse.ArgumentParser(description="Generate megcombined.json for ModelEngine items")
    parser.add_argument("--dry-run", action="store_true", help="Simulate actions without writing files")
    args = parser.parse_args()

    for folder_name in os.listdir(ITEMS_DIR):
        folder_path = os.path.join(ITEMS_DIR, folder_name)
        if not os.path.isdir(folder_path):
            continue
        if not FILTER_REGEX.match(folder_name):
            continue
        generate_combined_model(folder_path, folder_name, dry_run=args.dry_run)

if __name__ == "__main__":
    main()
